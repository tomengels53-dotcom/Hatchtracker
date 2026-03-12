import os
import glob

files = glob.glob("core/ui/src/main/res/values*/strings.xml")
for f in files:
    with open(f, "r", encoding="utf-8") as file:
        lines = file.readlines()
    with open(f, "w", encoding="utf-8") as file:
        for line in lines:
            if 'name="breeding_planner_' not in line and 'name="breeding_scenario_' not in line:
                file.write(line)
print("Removed legacy strings from", len(files), "files")
