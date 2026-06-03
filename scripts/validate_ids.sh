#!/bin/bash
# ID Validator: Compare findViewById() calls in Java with android:id definitions in XML layouts

echo "=============================================================================="
echo "ID Validator: Checking findViewById() calls against layout XML definitions"
echo "=============================================================================="
echo ""

JAVA_DIR="app/src/main/java/br/jss/motoreviso"
LAYOUT_DIR="app/src/main/res/layout"
ERRORS=0

# Create temp directory for IDs
mkdir -p /tmp/id_validator
LAYOUT_IDS_DIR="/tmp/id_validator/layouts"
rm -rf "$LAYOUT_IDS_DIR"
mkdir -p "$LAYOUT_IDS_DIR"

# Extract IDs from all layouts
echo "Extracting IDs from ${LAYOUT_DIR}..."
for layout_file in "$LAYOUT_DIR"/*.xml; do
    layout_name=$(basename "$layout_file" .xml)
    grep -o 'android:id="@[^"]*id/[a-zA-Z_][a-zA-Z0-9_]*"' "$layout_file" | \
        sed 's/.*id\///' | sed 's/"//' | sort > "$LAYOUT_IDS_DIR/$layout_name.txt"
done

echo "Checking Java files..."
echo ""

# Check each Java file
for java_file in $(find "$JAVA_DIR" -name "*.java" -exec grep -l "findViewById" {} \;); do
    java_filename=$(basename "$java_file" .java)

    # Infer layout name from class name
    # Convert CamelCase to snake_case
    layout_name=$(echo "$java_filename" | sed 's/\([a-z]\)\([A-Z]\)/\1_\2/g' | tr '[:upper:]' '[:lower:]')

    # Also check if layout matches exactly
    if [ ! -f "$LAYOUT_IDS_DIR/$layout_name.txt" ]; then
        # Try without Activity/Fragment suffix
        layout_name_alt=$(echo "$layout_name" | sed 's/_activity$//' | sed 's/_fragment$//')
        if [ -f "$LAYOUT_IDS_DIR/$layout_name_alt.txt" ]; then
            layout_name="$layout_name_alt"
        fi
    fi

    # Skip if layout not found
    if [ ! -f "$LAYOUT_IDS_DIR/$layout_name.txt" ]; then
        echo "⚠️  $java_filename: Cannot infer layout name (expected: $layout_name.xml)"
        continue
    fi

    # Extract findViewById calls
    grep -o 'findViewById(R\.id\.[a-zA-Z_][a-zA-Z0-9_]*' "$java_file" | \
        sed 's/.*id\.//' | sort | uniq > /tmp/id_validator/findviews.txt

    # Compare with layout IDs
    layout_ids_file="$LAYOUT_IDS_DIR/$layout_name.txt"
    while IFS= read -r id; do
        [ -z "$id" ] && continue
        if ! grep -q "^${id}$" "$layout_ids_file" 2>/dev/null; then
            echo "❌ $java_filename ($layout_name.xml) - ID 'R.id.$id' NOT FOUND in layout"
            ((ERRORS++))
        fi
    done < /tmp/id_validator/findviews.txt
done

echo ""
echo "=============================================================================="
if [ $ERRORS -eq 0 ]; then
    echo "✅ All IDs are correctly defined!"
    echo "   Every findViewById() call has a matching android:id in the layout."
else
    echo "❌ Found $ERRORS missing ID(s)"
fi
echo "=============================================================================="

# Cleanup
rm -rf "$LAYOUT_IDS_DIR"
rm -f /tmp/id_validator/findviews.txt

exit $ERRORS
