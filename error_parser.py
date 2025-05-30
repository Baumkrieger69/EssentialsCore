#!/usr/bin/env python3
import json
import sys
from collections import defaultdict

def parse_errors(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Parse JSON array
    errors = json.loads(content)
    
    # Group errors by severity and file
    severity_groups = defaultdict(list)
    file_groups = defaultdict(list)
    
    for error in errors:
        severity = error.get('severity', 0)
        resource = error.get('resource', '')
        message = error.get('message', '')
        
        # Extract just the filename from full path
        filename = resource.split('/')[-1] if '/' in resource else resource
        
        severity_groups[severity].append(error)
        file_groups[filename].append(error)
    
    print("=== ERROR SUMMARY ===")
    print(f"Total errors: {len(errors)}")
    print("\nBy Severity:")
    for severity in sorted(severity_groups.keys(), reverse=True):
        count = len(severity_groups[severity])
        severity_name = {8: "ERROR", 4: "WARNING", 2: "INFO", 1: "HINT"}.get(severity, f"UNKNOWN({severity})")
        print(f"  {severity_name}: {count}")
    
    print("\nCritical Errors (Severity 8) by File:")
    critical_errors = severity_groups.get(8, [])
    critical_by_file = defaultdict(list)
    
    for error in critical_errors:
        resource = error.get('resource', '')
        filename = resource.split('/')[-1] if '/' in resource else resource
        critical_by_file[filename].append(error)
    
    for filename in sorted(critical_by_file.keys()):
        errors_in_file = critical_by_file[filename]
        print(f"  {filename}: {len(errors_in_file)} critical errors")
        
        # Show first few error messages for this file
        for i, error in enumerate(errors_in_file[:3]):
            line = error.get('startLineNumber', '?')
            message = error.get('message', '')
            print(f"    Line {line}: {message}")
        
        if len(errors_in_file) > 3:
            print(f"    ... and {len(errors_in_file) - 3} more")
        print()

if __name__ == "__main__":
    file_path = "/workspaces/EssentialsCore/temp/Textdokument (neu).txt"
    parse_errors(file_path)
