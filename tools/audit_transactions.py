import os
import glob
import re

def audit_transactions():
    daos = glob.glob('core/data-local/src/main/java/**/*Dao.kt', recursive=True)
    violations = []
    
    for dao in daos:
        with open(dao, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Split by blocks or just simple string check
        # We look for `suspend fun name(args) {`
        # Because Room DAOs are interfaces, a fun with `{` is an implementation, meaning multi-step.
        
        lines = content.split('\n')
        for i, line in enumerate(lines):
            # A very simple heuristic: fun with { at the end, or the next line has {
            if re.search(r'fun\s+\w+\s*\(.*(:\s*\w+[?]?)?\s*\{', line) or (re.search(r'fun\s+\w+\s*\(.*\)', line) and i+1 < len(lines) and '{' in lines[i+1]):
                # Check for @Transaction in previous 2-3 lines
                has_transaction = False
                for j in range(max(0, i-3), i+1):
                    if '@Transaction' in lines[j]:
                        has_transaction = True
                        break
                
                if not has_transaction:
                    violations.append(f"{dao}:{i+1} -> {line.strip()}")
                    
    for v in violations:
        print(v)
    if not violations:
        print("No missing @Transaction found on DAO implementations.")

if __name__ == '__main__':
    audit_transactions()
