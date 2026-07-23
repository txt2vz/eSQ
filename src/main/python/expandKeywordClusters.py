import argparse
import csv
import json
import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List

import pandas as pd
#from nltk.stem import PorterStemmer
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
    "CRISIS3": DatasetConfig(folder_path="Crisis3", description="Crisis3 dataset"),
    "CRISIS4": DatasetConfig(folder_path="Crisis4", description="Crisis4 dataset"),
    "CRISIS6": DatasetConfig(folder_path="CrisisLexT6", description="CrisisLexT6 dataset"),
    "R4": DatasetConfig(folder_path="R4", description="Reuters 4 dataset"),
    "R5": DatasetConfig(folder_path="R5", description="Reuters 5 dataset"),
    "R6": DatasetConfig(folder_path="R6", description="Reuters 6 dataset"),
    "BBC5": DatasetConfig(folder_path="BBC5", description="BBC News dataset with 5 classes"),
    "BBC4": DatasetConfig(folder_path="BBC4", description="BBC News dataset with 4 classes"),

}

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[2]
DATASETS_DIR = REPO_ROOT / "datasets"
KEYWORD_ROOT = REPO_ROOT / "Keywords_JSON"
RESULTS_DIR = REPO_ROOT / "results"
#RESULTS_CSV = RESULTS_DIR / "results_compareStemmed.csv"
RESULTS_CSV = RESULTS_DIR / "results_compare.csv"
#RESULTS_CSV = RESULTS_DIR / "results_compare6.csv"
#RESULTS_CSV = RESULTS_DIR / "results_R4_NG5_CRISIS6.csv"

#STEMMER = PorterStemmer()


#def normalize_text(text: str) -> str:
 #   """Tokenize and stem text while preserving hashtags as tokens."""
  #  tokens = re.findall(r"#\w+|\b\w+\b", text.lower())
   # stemmed_tokens = [STEMMER.stem(token) for token in tokens]
    #return " ".join(stemmed_tokens)
    #return " ".join(tokens)  # Return unstemmed tokens for comparison


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
                #normalized_text = normalize_text(raw)
                #documents.append(normalized_text)
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
       # normalized_doc = normalize_text(doc)
        # Match whole words only after stemming both the document text and the keyword terms.
       # matches = [
       #     i
        #    for i, keywords in enumerate(keyword_sets)
        #    if any(
         #      # re.search(rf"\b{re.escape(STEMMER.stem(keyword.lower()))}\b", normalized_doc)
         #       re.search(rf"\b{re.escape(  (keyword.lower()))}\b", normalized_doc)
          #      for keyword in keywords
           # )
       # ]

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
        candidates.extend([REPO_ROOT / path, KEYWORD_ROOT / path, Path.cwd() / path])

    for candidate in candidates:
        if candidate.exists():
            return candidate

    raise FileNotFoundError(
        f"Keyword file not found: '{file_path}'. Checked paths: {', '.join(str(c) for c in candidates)}"
    )


def resolve_keyword_dir_path(dir_path: str) -> Path:
    path = Path(dir_path)
    candidates = [path] if path.is_absolute() else [REPO_ROOT / path, KEYWORD_ROOT / path, Path.cwd() / path]

    for candidate in candidates:
        if candidate.exists() and candidate.is_dir():
            return candidate

    raise FileNotFoundError(
        f"Keyword directory not found: '{dir_path}'. Checked paths: {', '.join(str(c) for c in candidates)}"
    )


def load_keyword_file(file_path: str | Path) -> tuple[str, List[List[str]]]:
    resolved_path = resolve_keyword_file_path(file_path)
    with resolved_path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    if not isinstance(data, dict):
        raise ValueError("Keyword JSON must be an object with 'dataSetName' and 'keyWordClusters'.")

    dataset_name = data.get("dataSetName")
    if not isinstance(dataset_name, str) or not dataset_name.strip():
        raise ValueError("Keyword JSON must include a non-empty 'dataSetName' string.")

    keyword_sets = data.get("keyWordClusters")
    if not isinstance(keyword_sets, list) or not all(
        isinstance(cluster, list) and all(isinstance(item, str) for item in cluster)
        for cluster in keyword_sets
    ):
        raise ValueError("Keyword JSON must include 'keyWordClusters' as a list of keyword lists.")

    return dataset_name.strip(), keyword_sets


def resolve_dataset_folder(folder_path: str | Path) -> Path:
    folder = Path(folder_path)
    if not folder.is_absolute():
        folder = DATASETS_DIR / folder
    if not folder.exists():
        raise FileNotFoundError(
            f"Dataset folder not found for '{folder_path}'. "
            f"Expected path: {folder}"
        )
    return folder


def get_classifier_list() -> List[tuple[str, object]]:
    """Create a small set of classifiers suitable for sparse text features."""
    return [
        ("ComplementNB", ComplementNB()),
     #   ("MultinomialNB", MultinomialNB()),
     #   ("LinearSVC", LinearSVC(random_state=42, class_weight="balanced")),
     #   ("KNeighbors", KNeighborsClassifier(n_neighbors=5, weights="distance")),
    ]


def evaluate_expanded_classifiers(
    classifiers: List[tuple[str, object]],
    X_train,
    y_train,
    X,
    labels: List[str],
) -> tuple[Dict[str, float], Dict[str, float], tuple[str, float, float] | None]:
    """Fit each classifier on the seed clusters and evaluate its predictions over all documents."""
    classifier_ari: Dict[str, float] = {}
    classifier_v: Dict[str, float] = {}
    best_result: tuple[str, float, float] | None = None

    for name, classifier in classifiers:
        fitted_classifier = classifier.fit(X_train, y_train)
        predicted_clusters = fitted_classifier.predict(X)
        v_measure, ari = evaluate_clustering(labels, predicted_clusters)
        classifier_ari[name] = ari
        classifier_v[name] = v_measure

        if best_result is None or v_measure > best_result[1] or (
            v_measure == best_result[1] and ari > best_result[2]
        ):
            best_result = (name, v_measure, ari)

    return classifier_ari, classifier_v, best_result


def write_results(
    *,
    dataset_name: str,
    keyword_file_name: str,
    num_classes: int,
    num_clusters: int,
    base_ari: float,
    base_v: float,
    classifier_ari: Dict[str, float] | None = None,
    classifier_v: Dict[str, float] | None = None,
) -> None:
    """Append one experiment row to the CSV results file.

    This function collects the metrics from one run of the keyword-clustering pipeline
    and stores them in a single row of a CSV file. The row contains the core experiment
    metadata (dataset name, class count, cluster count, and base clustering scores) plus
    one or more classifier-specific columns for the expanded-classifier evaluation.
    """
    # Ensure the output directory exists before trying to write a results file.
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    results_path = RESULTS_CSV

    # Build a dictionary representing one row of CSV data.
    # Each key becomes a column name in the output file, and each value stores the
    # corresponding metric or metadata for this experiment.
    row: Dict[str, object] = {
        "dataset_name": dataset_name,
    }
     
    # Add one column per classifier score for ARI metrics.
    # The column name is derived from the classifier name, with non-alphanumeric
    # characters replaced by underscores, and the suffix "_ARI" is appended.
    if classifier_ari:
        for name, score in classifier_ari.items():
            row[f"{re.sub(r'[^0-9A-Za-z]+', '_', name)}_ARI"] = score

    # Add one column per classifier score for V-measure metrics.
    # The column name is derived from the classifier name, again with non-alphanumeric
    # characters replaced by underscores, and the suffix "_V" is appended.
    if classifier_v:
        for name, score in classifier_v.items():
            row[f"{re.sub(r'[^0-9A-Za-z]+', '_', name)}_V"] = score

    row.update({         
        "num_classes": num_classes,
        "num_clusters": num_clusters,
        "base_ARI": base_ari,
        "base_V": base_v,
        "keyword_file_name": keyword_file_name,
        "timestamp": datetime.now().isoformat(),
    })    

    # If a results CSV already exists and is non-empty, read it and append the new row.
    # This preserves the existing columns and adds a new record beneath them.
    if results_path.exists() and results_path.stat().st_size > 0:
        existing_df = pd.read_csv(results_path)
        combined_df = pd.concat([existing_df, pd.DataFrame([row])], ignore_index=True)
    else:
        # If the file does not exist yet, create a one-row DataFrame from the current row.
        # When pandas writes this DataFrame to CSV, the dictionary keys become the header row.
        combined_df = pd.DataFrame([row])

    # Write the combined data back to disk, without adding an index column.
    combined_df.to_csv(results_path, index=False)


def run_experiment(keyword_file: str | Path, dataset_filter: str | None = None, strict: bool = False) -> None:
    resolved_keyword_path = resolve_keyword_file_path(keyword_file)
    dataset_name, keyword_sets = load_keyword_file(resolved_keyword_path)

    if dataset_filter and dataset_name != dataset_filter:
        if strict:
            raise ValueError(
                f"Keyword file {resolved_keyword_path.name} contains dataSetName '{dataset_name}', "
                f"but requested dataset was '{dataset_filter}'."
            )
        return

    if dataset_name not in DATASETS:
        raise ValueError(
            f"Keyword file {resolved_keyword_path.name} contains dataSetName '{dataset_name}', "
            f"but no matching dataset is configured. Available datasets: {sorted(DATASETS)}"
        )

    dataset = DATASETS[dataset_name]
    print(f"Using dataset '{dataset_name}' from folder '{dataset.folder_path}'")
    print(f"Using keyword file '{resolved_keyword_path}'")

    dataset_folder = resolve_dataset_folder(dataset.folder_path)
    documents, labels = load_documents(dataset_folder)

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
        run_experiment(json_file, dataset_filter=dataset_filter)


def overallResults():
    if not RESULTS_CSV.exists() or RESULTS_CSV.stat().st_size == 0:
        print(f"No results file found at {RESULTS_CSV}. Skipping overall results.")
        return

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
    print(f"Average clusterCountError: {cluster_count_error_mean:.4f}")
    print()
    print("Average V-measure by classifier:")
    print(v_means)
    print(f"Best classifier by V-measure: {best_v_classifier} with average V = {best_v_value:.4f}")
    print()      
    print("Average ARI by classifier:")
    print(ari_means)
    print(f"Best classifier by ARI: {best_ari_classifier} with average ARI = {best_ari_value:.4f}")  


def main() -> None:
    args = parse_args()
    if args.list:
        list_datasets()
        return

    if args.keyword_file:
        run_experiment(args.keyword_file, dataset_filter=args.dataset, strict=bool(args.dataset))
    else:
        process_keyword_directory(args.keyword_dir, args.dataset)

    overallResults()


if __name__ == "__main__":
    main()


