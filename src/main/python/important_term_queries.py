import argparse
import math
import re
from collections import Counter, defaultdict
from pathlib import Path

TOKEN_RE = re.compile(r"[A-Za-z0-9]+")
DEFAULT_STOP_WORDS_PATH = Path("src/cfg/stop_words_moderate.txt")
DEFAULT_DATASET_PATH = Path("datasets/NG3")
MAX_TERMQUERYLIST_SIZE = 120


def load_stop_words(path: Path) -> set[str]:
    if not path.exists():
        raise FileNotFoundError(f"Stop words file not found: {path}")

    return {
        line.strip().lower()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    }


def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_RE.findall(text)]


def is_useful_term(term: str, doc_freq: int, stop_words: set[str]) -> bool:
    if doc_freq < 4:
        return False
    if len(term) < 2:
        return False
    if not term[0].isalpha():
        return False
    if any(not ch.isalnum() for ch in term):
        return False
    if term in stop_words:
        return False
    return True


def read_documents(dataset_dir: Path) -> list[str]:
    if not dataset_dir.exists() or not dataset_dir.is_dir():
        raise FileNotFoundError(f"Dataset directory not found: {dataset_dir}")

    docs: list[str] = []
    for path in sorted(dataset_dir.rglob("*")):
        if not path.is_file():
            continue
        if path.name.startswith("."):
            continue
        try:
            docs.append(path.read_text(encoding="utf-8", errors="ignore"))
        except Exception:
            continue
    return docs


def get_tfidf_term_list_from_corpus(
    dataset_dir: Path,
    max_size: int = MAX_TERMQUERYLIST_SIZE,
    stop_words_file: Path = DEFAULT_STOP_WORDS_PATH,
) -> list[tuple[str, float]]:
    stop_words = load_stop_words(stop_words_file)
    documents = read_documents(dataset_dir)
    total_docs = len(documents)

    if total_docs == 0:
        return []

    doc_term_counts: list[Counter[str]] = []
    term_doc_frequency: defaultdict[str, int] = defaultdict(int)

    for text in documents:
        tokens = [token for token in tokenize(text) if token not in stop_words]
        counter = Counter(tokens)
        doc_term_counts.append(counter)
        for term in counter.keys():
            term_doc_frequency[term] += 1

    term_scores: dict[str, float] = {}
    for term, df in term_doc_frequency.items():
        if not is_useful_term(term, df, stop_words):
            continue

        idf = math.log(total_docs / df) + 1.0
        tfidf_total = 0.0
        for counter in doc_term_counts:
            term_freq = counter.get(term, 0)
            if term_freq <= 0:
                continue
            tf = math.sqrt(term_freq)
            tfidf_total += tf * (2.0 - idf)

        term_scores[term] = tfidf_total

    sorted_terms = sorted(term_scores.items(), key=lambda kv: kv[1])
    return sorted_terms[:max_size]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Compute important terms from a document corpus using TF-IDF scoring."
    )
    parser.add_argument(
        "--dataset-dir",
        type=Path,
        default=DEFAULT_DATASET_PATH,
        help="Path to the dataset directory containing subfolders and text documents.",
    )
    parser.add_argument(
        "--stop-words",
        type=Path,
        default=DEFAULT_STOP_WORDS_PATH,
        help="Path to the stop words file.",
    )
    parser.add_argument(
        "--max-size",
        type=int,
        default=MAX_TERMQUERYLIST_SIZE,
        help="Maximum number of important terms to return.",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=40,
        help="Number of top terms to print with scores.",
    )
    args = parser.parse_args()

    term_list = get_tfidf_term_list_from_corpus(
        args.dataset_dir,
        max_size=args.max_size,
        stop_words_file=args.stop_words,
    )

    print("Important words:")
    print(" ".join(term for term, _ in term_list[: args.top]))
    print(f"Term list size: {len(term_list)}")
    print("\nTop scores:")
    for term, score in term_list[: args.top]:
        print(f"{term}\t{score:.6f}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
