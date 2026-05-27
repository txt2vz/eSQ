import argparse
import csv
import json
import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List

import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import adjusted_rand_score, v_measure_score
from sklearn.naive_bayes import ComplementNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.svm import LinearSVC


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
    "crisis6": DatasetConfig(folder_path="CrisisLexT6", description="CrisisLexT6 dataset"),
    "R4": DatasetConfig(folder_path="R4", description="Reuters 4 dataset"),
    "R5": DatasetConfig(folder_path="R5", description="Reuters 5 dataset"),
    "R6": DatasetConfig(folder_path="R6", description="Reuters 6 dataset"),
}

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[2]
DATASETS_DIR = REPO_ROOT / "datasets"
KEYWORD_ROOT = REPO_ROOT / "Keywords_JSON"
RESULTS_DIR = REPO_ROOT / "results"
RESULTS_CSV = RESULTS_DIR / "results_compare.csv"

def load_documents(folder_path: str | Path) -> tuple[List[str], List[str]]:
    """Load documents recursively from a folder and extract labels from subfolder names.

    Prints the number of load errors and the number of files loaded per class.
    """
    folder = Path(folder_path)
    documents: List[str] = []
    labels: List[str] = []
    error_count = 0
    class_counts: Dict[str, int] = {}

    for file_path in folder.rglob("*"):
        if not file_path.is_file():
            continue
        try:
            with file_path.open("r", encoding="utf-8", errors="ignore") as f:
                raw = f.read()
                #tokens = re.findall(r"\b\w+\b", raw.lower())
                tokens = re.findall(r"#\w+|\b\w+\b", raw.lower()) #for hashtags as tokens
                documents.append(" ".join(tokens))
            label = file_path.parent.name
            labels.append(label)
            class_counts[label] = class_counts.get(label, 0) + 1
        except Exception as e:
            error_count += 1
            print(f"Error reading {file_path}: {e}")

    total_loaded = len(documents)
    print(f"Loaded {total_loaded} documents from '{folder}'. Errors: {error_count}")
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


def list_datasets() -> None:
    print("Available datasets:")
    for name, config in DATASETS.items():
        print(f"  {name}: {config.description}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Cluster documents using dataset-specific keyword sets.")
    parser.add_argument(
        "--dataset",
        choices=list(DATASETS.keys()),
        help="Optionally restrict processing to a single dataset when running a folder of JSON files.",
    )
    parser.add_argument(
        "--keyword-file",
        help="Load keyword sets from a single JSON file.",
    )
    parser.add_argument(
        "--keyword-dir",
        default=str(KEYWORD_ROOT),
        help="Folder containing JSON keyword files to process when --keyword-file is not provided.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="List available datasets.",
    )
    return parser.parse_args()


def resolve_keyword_file_path(file_path: str) -> Path:
    path = Path(file_path)
    candidates = [path]
    if not path.is_absolute():
        candidates.extend([REPO_ROOT / path, KEYWORD_ROOT / path])

    for candidate in candidates:
        if candidate.exists():
            return candidate

    raise FileNotFoundError(
        f"Keyword file not found: '{file_path}'. Checked paths: {', '.join(str(c) for c in candidates)}"
    )


def resolve_keyword_dir_path(dir_path: str) -> Path:
    path = Path(dir_path)
    candidates = [path] if path.is_absolute() else [REPO_ROOT / path, KEYWORD_ROOT / path]

    for candidate in candidates:
        if candidate.is_dir():
            return candidate

    raise FileNotFoundError(
        f"Keyword directory not found: '{dir_path}'. Checked paths: {', '.join(str(c) for c in candidates)}"
    )


def load_keyword_sets_from_file(file_path: str | Path) -> List[List[str]]:
    resolved_path = resolve_keyword_file_path(file_path)
    with resolved_path.open("r", encoding="utf-8") as file:
        data = json.load(file)

    if not isinstance(data, list) or not all(isinstance(group, list) for group in data):
        raise ValueError("Keyword file must contain a JSON array of keyword lists.")

    for group in data:
        if not all(isinstance(keyword, str) for keyword in group):
            raise ValueError("Each keyword must be a string inside keyword groups.")

    return data


def get_classifier_list():
    return [
        ("KNN", KNeighborsClassifier(n_neighbors=5)),
        ("LinearSVC", LinearSVC(max_iter=2000)),
        ("RandomForest", RandomForestClassifier(n_estimators=200, n_jobs=-1, random_state=42)),
        ("ComplementNB", ComplementNB()),
        ("MultinomialNB", MultinomialNB()),
    ]


def evaluate_expanded_classifiers(classifiers, X_train, y_train, X_all, labels):
    classifier_ari: Dict[str, float] = {}
    classifier_v: Dict[str, float] = {}
    best_score = None
    best_result = None

    for name, clf in classifiers:
        clf.fit(X_train, y_train)
        pred_labels_all = clf.predict(X_all)
        v_measure_expanded, ari_expanded = evaluate_clustering(labels, pred_labels_all)
        classifier_ari[name] = ari_expanded
        classifier_v[name] = v_measure_expanded
        print(f"{name} expanded clusters - V-measure: {v_measure_expanded:.4f}, ARI: {ari_expanded:.4f}")

        score = (v_measure_expanded + ari_expanded) / 2
        if best_score is None or score > best_score:
            best_score = score
            best_result = (name, v_measure_expanded, ari_expanded)

    return classifier_ari, classifier_v, best_result


def write_results(
    dataset_name: str,
    keyword_file_name: str,
    num_classes: int,
    num_clusters: int,
    base_ari: float,
    base_v: float,
    classifier_ari: Dict[str, float],
    classifier_v: Dict[str, float],
) -> None:
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    classifier_names = list(classifier_ari)
    header = [
        "dataset",
    ] + [f"{name}_ARI" for name in classifier_names] + [f"{name}_V" for name in classifier_names] + [
        "keyword_file",
        "num_classes",
        "num_clusters",
        "clusterCountError",
        "base_ARI",
        "base_V",
        "timestamp",
    ]

    cluster_count_error = abs(num_classes - num_clusters)
    row = [
        dataset_name,
    ] + [f"{classifier_ari[name]:.6f}" for name in classifier_names] + [f"{classifier_v[name]:.6f}" for name in classifier_names] + [
        keyword_file_name,
        str(num_classes),
        str(num_clusters),
        str(cluster_count_error),
        f"{base_ari:.6f}",
        f"{base_v:.6f}",
        f"{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
    ]

    mode = "w" if not RESULTS_CSV.exists() or RESULTS_CSV.stat().st_size == 0 else "a"
    with RESULTS_CSV.open(mode, newline="", encoding="utf-8") as csvfile:
        writer = csv.writer(csvfile)
        if mode == "w":
            writer.writerow(header)
        writer.writerow(row)

    print(f"Results appended to {RESULTS_CSV}")


def resolve_dataset_folder(folder_name: str) -> Path:
    path = DATASETS_DIR / folder_name
    if path.is_dir():
        return path
    raise FileNotFoundError(
        f"Dataset folder not found: '{folder_name}'. Expected under {DATASETS_DIR}"
    )


def infer_dataset_name_from_keyword_file(file_name: str) -> str | None:
    patterns = [
        r"^(.+?)_keywordSet_\d+\.json$",
        r"^best_query_(.+?)_job_\d+\.json$",
    ]
    for pattern in patterns:
        match = re.match(pattern, file_name, re.IGNORECASE)
        if match:
            candidate = match.group(1)
            for dataset_name in DATASETS:
                if dataset_name.lower() == candidate.lower():
                    return dataset_name

    for dataset_name in DATASETS:
        if file_name.lower().startswith(dataset_name.lower() + "_"):
            return dataset_name

    for dataset_name in DATASETS:
        if dataset_name.lower() in file_name.lower():
            return dataset_name

    return None


def run_experiment(dataset_name: str, keyword_file: str) -> None:
    dataset = DATASETS[dataset_name]
    print(f"Using dataset '{dataset_name}' from folder '{dataset.folder_path}'")
    print(f"Using keyword file '{keyword_file}'")

    dataset_folder = resolve_dataset_folder(dataset.folder_path)
    documents, labels = load_documents(dataset_folder)
    resolved_keyword_path = resolve_keyword_file_path(keyword_file)
    keyword_sets = load_keyword_sets_from_file(resolved_keyword_path)

    print("Keyword sets used:")
    for idx, keywords in enumerate(keyword_sets):
        print(f"  cluster {idx}: {keywords}")

    base_clusters = create_base_clusters(documents, keyword_sets)
    clustered_indices = sorted(index for cluster in base_clusters.values() for index in cluster)
    doc_to_cluster = {index: cluster_id for cluster_id, cluster in base_clusters.items() for index in cluster}

    true_labels_clustered = [labels[i] for i in clustered_indices]
    pred_labels_clustered = [doc_to_cluster[i] for i in clustered_indices]

    v_measure_base, ari_base = evaluate_clustering(true_labels_clustered, pred_labels_clustered)
    print(f"Base clusters - V-measure: {v_measure_base:.4f}, ARI: {ari_base:.4f}")

    vectorizer = TfidfVectorizer(
        stop_words="english",
        max_df=0.5,
        min_df=2,
        ngram_range=(1, 2),
        max_features=30000,
    )
    X = vectorizer.fit_transform(documents)

    X_train = X[clustered_indices]
    y_train = pred_labels_clustered

    classifiers = get_classifier_list()
    classifier_ari, classifier_v, best_result = evaluate_expanded_classifiers(classifiers, X_train, y_train, X, labels)

    if best_result is not None:
        name, v_best, ari_best = best_result
        print(f"Best expanded classifier: {name} -> V-measure: {v_best:.4f}, ARI: {ari_best:.4f}")
    else:
        name = "base"
        v_best, ari_best = v_measure_base, ari_base

    write_results(
        dataset_name=dataset_name,
        keyword_file_name=Path(resolved_keyword_path).name,
        num_classes=len(set(labels)),
        num_clusters=len(keyword_sets),
        base_ari=ari_base,
        base_v=v_measure_base,
        classifier_ari=classifier_ari,
        classifier_v=classifier_v,
    )


def process_keyword_directory(keyword_dir: str, dataset_filter: str | None = None) -> None:
    resolved_dir = resolve_keyword_dir_path(keyword_dir)
    json_files = sorted(
        file_path for file_path in resolved_dir.iterdir()
        if file_path.is_file() and file_path.suffix.lower() == '.json'
    )

    if not json_files:
        raise FileNotFoundError(f"No JSON keyword files found in directory '{resolved_dir}'")

    for json_file in json_files:
        dataset_name = infer_dataset_name_from_keyword_file(json_file.name)
        if dataset_name is None:
            print(f"Skipping file with unknown dataset prefix: {json_file.name}")
            continue
        if dataset_filter and dataset_name != dataset_filter:
            continue

        run_experiment(dataset_name, json_file)

def overallResults():    

    df = pd.read_csv(RESULTS_CSV)

    # classifier ARI and V columns
    ari_cols = [col for col in df.columns if col.endswith("_ARI") and col != "base_ARI"]
    v_cols = [col for col in df.columns if col.endswith("_V") and col != "base_V"]

    ari_means = df[ari_cols].mean()
    v_means = df[v_cols].mean()
    cluster_count_error_mean = df["clusterCountError"].mean() if "clusterCountError" in df.columns else float("nan")
    best_ari_classifier = ari_means.idxmax()
    best_ari_value = ari_means.max()
    best_v_classifier = v_means.idxmax()
    best_v_value = v_means.max()
    
    print()
    print("********************************************************")
    print("Average V-measure by classifier:")
    print(v_means)
    print(f"Best classifier by V-measure: {best_v_classifier} with average V = {best_v_value:.4f}")
    print()
      
    print("Average ARI by classifier:")
    print(ari_means)
    print(f"Best classifier by ARI: {best_ari_classifier} with average ARI = {best_ari_value:.4f}")
    print()
    print(f"Average clusterCountError: {cluster_count_error_mean:.4f}")


def main() -> None:
    args = parse_args()
    if args.list:
        list_datasets()
        return

    if args.keyword_file:
        if args.dataset:
            run_experiment(args.dataset, args.keyword_file)       
        else:
            file_name = Path(args.keyword_file).name
            dataset_name = infer_dataset_name_from_keyword_file(file_name)
            if dataset_name is None:
                raise ValueError(
                    f"Cannot infer dataset from keyword file name '{file_name}'. Provide --dataset explicitly."
                )
            run_experiment(dataset_name, args.keyword_file)
    else:
        process_keyword_directory(args.keyword_dir, args.dataset)

    overallResults()


if __name__ == "__main__":
    main()
    

