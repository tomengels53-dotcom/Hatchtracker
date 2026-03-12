import os
import glob
import re

classes_to_annotate = [
    "AddDeviceUiState",
    "AdminAuditLogUiState",
    "BreedAdminUiState",
    "BreedingHistoryUiState",
    "BreedingProgramWizardUiState",
    "BreedingSelectionUiState",
    "FlockRowModel",
    "HatchPlannerUiState",
    "HatchyChatUiState",
    "IncubationRowModel",
    "ManualFlockletState",
    "NurseryRowModel",
    "OptimizationUiState",
    "SupportUiState"
]

def add_immutable_annotation():
    kotlin_files = glob.glob('**/*.kt', recursive=True)
    files_modified = 0
    
    # We want to match data class X or class X
    pattern_template = r'(?P<indent>^[ \t]*)(data class|class) (?P<class_name>{})([\s\(])'
    
    for kt_file in kotlin_files:
        if 'build' in kt_file or '_backup' in kt_file or '_monolith' in kt_file:
            continue
            
        with open(kt_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            
        modified = False
        new_content = content
        
        for cls in classes_to_annotate:
            pattern = pattern_template.format(cls)
            # Find the match to see if it exists
            if re.search(pattern, new_content, flags=re.MULTILINE):
                # Check if it already has @Immutable or @androidx.compose.runtime.Immutable
                # A simple check: if the file already has androidx.compose.runtime.Immutable for this class
                # We will just do a regex sub.
                
                def replacer(match):
                    indent = match.group('indent')
                    type_decl = match.group(2)
                    name = match.group('class_name')
                    tail = match.group(4)
                    
                    # Avoid double annotation
                    # We can't easily check previous line in this regex, but we can just prepend.
                    return f"{indent}@androidx.compose.runtime.Immutable\n{indent}{type_decl} {name}{tail}"
                
                # We need to make sure we don't annotate twice.
                if f"@androidx.compose.runtime.Immutable\n{cls}" not in new_content.replace(' ', ''):
                     new_content = re.sub(pattern, replacer, new_content, flags=re.MULTILINE)
                     modified = True
                     print(f"Annotated {cls} in {kt_file}")
                     
        if modified:
            with open(kt_file, 'w', encoding='utf-8', errors='ignore') as f:
                f.write(new_content)
            files_modified += 1

    print(f"Total files modified: {files_modified}")

if __name__ == '__main__':
    add_immutable_annotation()
