import os

files_to_patch = {
    'core/data/src/main/java/com/example/hatchtracker/data/worker/FinancialRecurrenceWorker.kt': (
        ".setRequiredNetworkType(NetworkType.CONNECTED)",
        ".setRequiredNetworkType(NetworkType.CONNECTED)\n                    .setRequiresBatteryNotLow(true)"
    ),
    'core/data/src/main/java/com/example/hatchtracker/data/worker/CostAccountingWorker.kt': (
        ".setConstraints(Constraints.Builder()\n                    .build())",
        ".setConstraints(Constraints.Builder()\n                    .setRequiresBatteryNotLow(true)\n                    .build())"
    )
}

for filepath, (target, replacement) in files_to_patch.items():
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    if target in content and replacement not in content:
        new_content = content.replace(target, replacement)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Patched {filepath}")
    else:
        print(f"Skipped {filepath} - target not found or already patched")
