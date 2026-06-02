#!/usr/bin/env python3
"""
ID Validator: Compare findViewById() calls in Java with android:id definitions in XML layouts
Usage: python3 validate_ids.py
"""

import re
import os
from pathlib import Path
from collections import defaultdict

# Project paths
JAVA_DIR = "app/src/main/java/br/jss/motoreviso"
LAYOUT_DIR = "app/src/main/res/layout"

def extract_ids_from_layout(layout_file):
    """Extract all android:id values from an XML layout file."""
    ids = set()
    try:
        with open(layout_file, 'r', encoding='utf-8') as f:
            content = f.read()
            # Find all android:id="@+id/xxx" or android:id="@id/xxx"
            matches = re.findall(r'android:id="@(?:\+)?id/([a-zA-Z_]\w*)"', content)
            ids.update(matches)
    except Exception as e:
        print(f"❌ Error reading {layout_file}: {e}")
    return ids

def extract_findviewbyid_calls(java_file):
    """Extract all findViewById(R.id.xxx) calls from a Java file."""
    ids = set()
    try:
        with open(java_file, 'r', encoding='utf-8') as f:
            content = f.read()
            # Find all findViewById(R.id.xxx) patterns
            matches = re.findall(r'findViewById\(R\.id\.([a-zA-Z_]\w*)\)', content)
            ids.update(matches)
    except Exception as e:
        print(f"❌ Error reading {java_file}: {e}")
    return ids

def get_layout_name_from_java(java_filename, java_content):
    """Infer layout name from R.layout.xxx or from class name."""
    # Try to find R.layout.xxx pattern
    match = re.search(r'R\.layout\.([a-z_]+)', java_content)
    if match:
        return match.group(1)

    # Fallback: convert class name to layout name
    # e.g., LoginActivity -> activity_login
    class_name = Path(java_filename).stem
    layout_name = re.sub(r'([a-z])([A-Z])', r'\1_\2', class_name).lower()
    return layout_name

def main():
    print("=" * 80)
    print("ID Validator: Checking findViewById() calls against layout XML definitions")
    print("=" * 80)

    # Map of layout file -> set of IDs
    layout_ids = {}
    for layout_file in Path(LAYOUT_DIR).glob("*.xml"):
        layout_name = layout_file.stem
        ids = extract_ids_from_layout(str(layout_file))
        layout_ids[layout_name] = ids

    print(f"\n✅ Found {len(layout_ids)} layout files")

    # Check Java files
    errors = []
    warnings = []
    total_checks = 0

    for java_file in Path(JAVA_DIR).rglob("*.java"):
        with open(java_file, 'r', encoding='utf-8') as f:
            content = f.read()

        # Skip if no findViewById calls
        if 'findViewById' not in content:
            continue

        java_filename = java_file.stem
        layout_name = get_layout_name_from_java(java_filename, content)

        # Extract findViewById calls
        findview_ids = extract_findviewbyid_calls(str(java_file))

        if not findview_ids:
            continue

        total_checks += len(findview_ids)

        # Check if layout exists
        if layout_name not in layout_ids:
            warnings.append(f"⚠️  {java_filename}: Inferred layout '{layout_name}' not found")
            continue

        available_ids = layout_ids[layout_name]

        # Find missing IDs
        missing_ids = findview_ids - available_ids
        if missing_ids:
            for missing_id in sorted(missing_ids):
                error_msg = (
                    f"❌ {java_filename} ({layout_name}.xml) - "
                    f"ID 'R.id.{missing_id}' not found in layout"
                )
                errors.append(error_msg)

    # Print results
    print(f"\n📋 Checked {total_checks} findViewById() calls\n")

    if errors:
        print("ERRORS (Missing IDs in layouts):")
        for error in sorted(errors):
            print(f"  {error}")
        print()

    if warnings:
        print("WARNINGS:")
        for warning in sorted(warnings):
            print(f"  {warning}")
        print()

    if not errors and not warnings:
        print("✅ All IDs are correctly defined!")
        print("   Every findViewById() call has a matching android:id in the layout.")

    print("=" * 80)
    return len(errors) == 0

if __name__ == '__main__':
    success = main()
    exit(0 if success else 1)
