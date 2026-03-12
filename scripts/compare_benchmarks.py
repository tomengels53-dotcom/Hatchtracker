import json
import sys
import os

def parse_benchmark(file_path):
    """
    Parses a Macrobenchmark JSON output file.
    Structure: { "benchmarks": [ { "name": "...", "metrics": { "metricName": { "median": ... } } } ] }
    """
    with open(file_path, 'r') as f:
        data = json.load(f)
    results = {}
    for benchmark in data.get('benchmarks', []):
        name = benchmark.get('name')
        metrics = benchmark.get('metrics', {})
        for metric_name, values in metrics.items():
            # Handle both old and new JSON formats
            val = values.get('median') or values.get('P50')
            if val is not None:
                key = f"{name}_{metric_name}"
                results[key] = val
    return results

def main():
    if len(sys.argv) < 3:
        print("Usage: python compare_benchmarks.py <baseline_json> <current_json> [threshold_percent]")
        sys.exit(1)

    baseline_file = sys.argv[1]
    current_file = sys.argv[2]
    threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 10.0

    if not os.path.exists(baseline_file):
        print(f"Baseline file missing: {baseline_file}. Initializing baseline.")
        # Optional: Copy current to baseline here in CI
        sys.exit(0) 

    if not os.path.exists(current_file):
        print(f"Current results file missing: {current_file}")
        sys.exit(1)

    try:
        baseline = parse_benchmark(baseline_file)
        current = parse_benchmark(current_file)
    except Exception as e:
        print(f"Error parsing JSON: {e}")
        sys.exit(1)

    failed = False
    print(f"\n[CI PERFORMANCE GUARD] Comparing benchmarks (Threshold: {threshold}%)")
    print("-" * 85)
    print(f"{'Metric':<45} | {'Baseline':>10} | {'Current':>10} | {'Diff':>8} | {'Status'}")
    print("-" * 85)
    
    for key, current_val in current.items():
        if key in baseline:
            base_val = baseline[key]
            if base_val and current_val:
                diff_percent = ((current_val - base_val) / base_val) * 100
                status = "PASS"
                if diff_percent > threshold:
                    status = "FAIL"
                    failed = True
                print(f"{key:<45} | {base_val:>8.1f}ms | {current_val:>8.1f}ms | {diff_percent:>+7.1f}% | {status}")
        else:
            print(f"{key:<45} | {'N/A':>10} | {current_val:>8.1f}ms | {'NEW':>8} | PASS")

    print("-" * 85)
    if failed:
        print("RESULT: FAIL (Performance regression detected)")
        sys.exit(1)
    else:
        print("RESULT: SUCCESS (Performance remains stable)")
        sys.exit(0)

if __name__ == "__main__":
    main()
