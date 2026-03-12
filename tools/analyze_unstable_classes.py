import os
import glob
import re

def analyze_compose_metrics():
    class_files = glob.glob('**/build/compose_metrics/*-classes.txt', recursive=True)
    unstable_classes = []
    
    for file in class_files:
        with open(file, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('unstable class'):
                    match = re.search(r'unstable class (\w+)', line)
                    if match:
                        class_name = match.group(1)
                        if class_name.endswith('UiState') or class_name.endswith('RowModel') or class_name.endswith('State'):
                            unstable_classes.append(class_name)
    
    print(f"Found {len(unstable_classes)} unstable UI State classes:")
    for c in sorted(set(unstable_classes)):
        print(c)

if __name__ == '__main__':
    analyze_compose_metrics()
