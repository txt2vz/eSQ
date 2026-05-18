import argparse
import json
import os
import re
from dataclasses import dataclass
from typing import Dict, List

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import adjusted_rand_score, pairwise_distances, v_measure_score
from sklearn.naive_bayes import ComplementNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import LinearSVC, SVC
from sklearn.neural_network import MLPClassifier


@dataclass
class DatasetConfig:
    folder_path: str
    description: str = ""


DATASETS: Dict[str, DatasetConfig] = {
    "NG5": DatasetConfig(folder_path="NG5", description="20 Newsgroups subset with 5 classes"),
    "NG6": DatasetConfig(folder_path="NG6", description="20 Newsgroups subset with 6 classes"),
    "NG3": DatasetConfig(folder_path="NG3", description="20 Newsgroups subset with 3 classes"),
    "crisis3": DatasetConfig(folder_path="crisis3", description="Crisis dataset"),
    "crisis4": DatasetConfig(folder_path="crisis4", description="Crisis4 dataset"),
    "R4": DatasetConfig(folder_path="R4", description="Reuters 4 dataset"),
    "R5": DatasetConfig(folder_path="R5", description="Reuters 5 dataset"),
    "R6": DatasetConfig(folder_path="R6", description="Reuters 6 dataset"),
}


def load_documents(folder_path: str) -> tuple[List[str], List[str]]:
    """Load documents recursively from a folder and extract labels from subfolder names.

    Prints the number of load errors and the number of files loaded per class.
    """
    documents: List[str] = []
    labels: List[str] = []
    error_count = 0
    class_counts: Dict[str, int] = {}

    for root, _, files in os.walk(folder_path):
        for file_name in files:
            file_path = os.path.join(root, file_name)
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    raw = f.read()
                    # Tokenize: keep only word characters and normalize to lowercase
                    tokens = re.findall(r"\b\w+\b", raw.lower())
                    documents.append(" ".join(tokens))
                label = os.path.basename(root)
                labels.append(label)
                class_counts[label] = class_counts.get(label, 0) + 1
            except Exception as e:
                error_count += 1
                print(f"Error reading {file_path}: {e}")

    total_loaded = len(documents)
    print(f"Loaded {total_loaded} documents from '{folder_path}'. Errors: {error_count}")
    if class_counts:
        print("Documents loaded per class:")
        for label, count in sorted(class_counts.items()):
            print(f"  {label}: {count}")
    return documents, labels


def create_base_clusters(documents: List[str], keyword_sets: List[List[str]]) -> Dict[int, List[int]]:
    """Create base clusters from documents using keyword sets.

    Returns only exclusive matches as base clusters, but prints both exclusive and total match counts per keyword set.
    """
    total_matches: Dict[int, int] = {i: 0 for i in range(len(keyword_sets))}
    base_clusters: Dict[int, List[int]] = {}

    for doc_index, doc in enumerate(documents):
        # Match whole words only. documents are already lowercased and tokenized.
        matches = [
            i
            for i, keywords in enumerate(keyword_sets)
            if any(re.search(rf"\b{re.escape(keyword.lower())}\b", doc) for keyword in keywords)
        ]
        for match_id in matches:
            total_matches[match_id] += 1
        if len(matches) == 1:
            cluster_id = matches[0]
            base_clusters.setdefault(cluster_id, []).append(doc_index)

    print("Document match counts per keyword set:")
    for set_id, keywords in enumerate(keyword_sets):
        exclusive_count = len(base_clusters.get(set_id, []))
        total_count = total_matches.get(set_id, 0)
        print(f"  cluster {set_id}: total matched {total_count}, exclusive matched {exclusive_count} - keywords={keywords}")

    return base_clusters


def evaluate_clustering(true_labels: List[str], pred_labels: List[int]) -> tuple[float, float]:
    """Compute V-measure and adjusted Rand index."""
    return v_measure_score(true_labels, pred_labels), adjusted_rand_score(true_labels, pred_labels)


class FuzzyKNN:
    """A simple fuzzy KNN classifier using distance-based membership weighting."""

    def __init__(self, n_neighbors: int = 5, m: float = 2.0):
        self.n_neighbors = n_neighbors
        self.m = m
        self.X_train = None
        self.y_train = None

    def fit(self, X, y):
        self.X_train = X
        self.y_train = list(y)
        return self

    def predict(self, X):
        distances = pairwise_distances(X, self.X_train, metric="euclidean")
        predictions = []

        for dist_row in distances:
            nearest_idx = dist_row.argsort()[: self.n_neighbors]
            nearest_dist = dist_row[nearest_idx]
            nearest_labels = [self.y_train[i] for i in nearest_idx]

            if any(d == 0 for d in nearest_dist):
                zero_dist_labels = [label for label, d in zip(nearest_labels, nearest_dist) if d == 0]
                predictions.append(max(set(zero_dist_labels), key=zero_dist_labels.count))
                continue

            weights = 1.0 / (nearest_dist ** (2.0 / (self.m - 1.0)))
            class_scores = {}
            for label, weight in zip(nearest_labels, weights):
                class_scores[label] = class_scores.get(label, 0.0) + weight

            predictions.append(max(class_scores, key=class_scores.get))

        return predictions


def list_datasets() -> None:
    print("Available datasets:")
    for name, config in DATASETS.items():
        print(f"  {name}: {config.description}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Cluster documents using dataset-specific keyword sets.")
    parser.add_argument(
        "--dataset",
        default="NG3",
        choices=list(DATASETS.keys()),
        help="Choose the dataset folder to load.",
    )
    parser.add_argument(
        "--keyword-file",
        required=True,
        help="Load keyword sets from a JSON file.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="List available datasets.",
    )
    return parser.parse_args()


def resolve_keyword_file_path(file_path: str) -> str:
    if os.path.isabs(file_path) and os.path.exists(file_path):
        return file_path

    candidates = [
        os.path.abspath(file_path),
        os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), file_path)),
        os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", file_path)),
    ]

    for candidate in candidates:
        if os.path.exists(candidate):
            return candidate

    raise FileNotFoundError(
        f"Keyword file not found: '{file_path}'. Checked paths: {', '.join(candidates)}"
    )


def load_keyword_sets_from_file(file_path: str) -> List[List[str]]:
    resolved_path = resolve_keyword_file_path(file_path)
    with open(resolved_path, "r", encoding="utf-8") as file:
        data = json.load(file)

    if not isinstance(data, list) or not all(isinstance(group, list) for group in data):
        raise ValueError("Keyword file must contain a JSON array of keyword lists.")

    for group in data:
        if not all(isinstance(keyword, str) for keyword in group):
            raise ValueError("Each keyword must be a string inside keyword groups.")

    return data


def resolve_dataset_folder(folder_path: str) -> str:
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", "..", ".."))
    candidates = [
        os.path.abspath(folder_path),
        os.path.abspath(os.path.join(script_dir, folder_path)),
        os.path.abspath(os.path.join(repo_root, folder_path)),
        os.path.abspath(os.path.join(repo_root, "datasets", folder_path)),
    ]

    for candidate in candidates:
        if os.path.isdir(candidate):
            return candidate

    raise FileNotFoundError(
        f"Dataset folder not found: '{folder_path}'. Checked paths: {', '.join(candidates)}"
    )


def run_experiment(dataset_name: str, keyword_file: str) -> None:
    dataset = DATASETS[dataset_name]
    print(f"Using dataset '{dataset_name}' from folder '{dataset.folder_path}'")
    print(f"Using keyword file '{keyword_file}'")

    dataset_folder = resolve_dataset_folder(dataset.folder_path)
    documents, labels = load_documents(dataset_folder)
    keyword_sets = load_keyword_sets_from_file(keyword_file)

    print("Keyword sets used:")
    for idx, keywords in enumerate(keyword_sets):
        print(f"  cluster {idx}: {keywords}")

    base_clusters = create_base_clusters(documents, keyword_sets)
    clustered_indices = sorted(index for cluster in base_clusters.values() for index in cluster)

    true_labels_clustered = [labels[i] for i in clustered_indices]
    pred_labels_clustered = [cluster_id for i in clustered_indices for cluster_id, indices in base_clusters.items() if i in indices]

    v_measure_base, ari_base = evaluate_clustering(true_labels_clustered, pred_labels_clustered)
    print(f"Base clusters - V-measure: {v_measure_base:.4f}, ARI: {ari_base:.4f}")

    vectorizer = TfidfVectorizer(
        stop_words="english",
        max_df=0.6,
        min_df=2,
        ngram_range=(1, 2),
        max_features=30000,
    )
    X = vectorizer.fit_transform(documents)

    X_train = X[clustered_indices]
    y_train = pred_labels_clustered

    classifiers = [
        ("KNN", KNeighborsClassifier(n_neighbors=5)),
        ("FuzzyKNN", FuzzyKNN(n_neighbors=5, m=2.0)),
        ("LogisticRegression", LogisticRegression(solver="lbfgs", max_iter=500)),
        ("LinearSVC", LinearSVC(max_iter=2000)),
        ("SVC-RBF", SVC(kernel="rbf", gamma="scale", max_iter=5000)),
        ("RandomForest", RandomForestClassifier(n_estimators=200, n_jobs=-1, random_state=42)),
        ("ComplementNB", ComplementNB()),
        ("MultinomialNB", MultinomialNB()),
    ]

    best_score = None
    best_result = None

    for name, clf in classifiers:
        X_train_fit = X_train
        X_predict = X

        clf.fit(X_train_fit, y_train)
        pred_labels_all = clf.predict(X_predict)
        v_measure_expanded, ari_expanded = evaluate_clustering(labels, pred_labels_all)
        print(f"{name} expanded clusters - V-measure: {v_measure_expanded:.4f}, ARI: {ari_expanded:.4f}")

        score = (v_measure_expanded + ari_expanded) / 2
        if best_score is None or score > best_score:
            best_score = score
            best_result = (name, v_measure_expanded, ari_expanded)

    if best_result is not None:
        name, v_best, ari_best = best_result
        print(f"Best expanded classifier: {name} -> V-measure: {v_best:.4f}, ARI: {ari_best:.4f}")


def main() -> None:
    args = parse_args()
    if args.list:
        list_datasets()
        return

    run_experiment(args.dataset, args.keyword_file)


if __name__ == "__main__":
    main()

