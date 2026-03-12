import os
import glob
import re

def optimize_callback_flows():
    kotlin_files = glob.glob('core/data/src/main/java/**/*.kt', recursive=True)
    files_modified = 0
    
    for kt_file in kotlin_files:
        with open(kt_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        modified = False
        new_content = content
        
        # We look for awaitClose { ... } followed by the closing brace of callbackFlow
        # And we want to replace `\n    }` with `\n    }.flowOn(Dispatchers.IO).distinctUntilChanged()`
        # We must be careful to only do this if it's not already done.
        
        if 'callbackFlow' in content and 'awaitClose' in content:
            # Add missing imports first if needed
            if 'kotlinx.coroutines.Dispatchers' not in new_content:
                new_content = new_content.replace('import kotlinx.coroutines.flow.callbackFlow', 
                                                  'import kotlinx.coroutines.flow.callbackFlow\nimport kotlinx.coroutines.flow.flowOn\nimport kotlinx.coroutines.flow.distinctUntilChanged\nimport kotlinx.coroutines.Dispatchers')
            elif 'kotlinx.coroutines.flow.flowOn' not in new_content:
                new_content = new_content.replace('import kotlinx.coroutines.flow.callbackFlow', 
                                                  'import kotlinx.coroutines.flow.callbackFlow\nimport kotlinx.coroutines.flow.flowOn\nimport kotlinx.coroutines.flow.distinctUntilChanged')
                
            # Regex to find: awaitClose { ... } \n    }
            # replace with: awaitClose { ... } \n    }.flowOn(Dispatchers.IO).distinctUntilChanged()
            # using a simple replace might be enough if formatting is consistent
            
            pattern = re.compile(r'(awaitClose\s*\{[^}]*\}\s*\n\s*)\}')
            
            def replacer(match):
                prefix = match.group(1)
                return f"{prefix}}}.flowOn(Dispatchers.IO).distinctUntilChanged()"
                
            # Only replace if not already replaced
            if '.flowOn(Dispatchers.IO)' not in new_content:
                new_content = pattern.sub(replacer, new_content)
                if new_content != content:
                    modified = True
                    print(f"Optimized flows in {kt_file}")
                    
        if modified:
            with open(kt_file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            files_modified += 1

    print(f"Total files modified: {files_modified}")

if __name__ == '__main__':
    optimize_callback_flows()
