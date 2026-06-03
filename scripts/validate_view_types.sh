#!/bin/bash
# View Type Validator: Check that findViewById() Java types match XML view types

echo "=============================================================================="
echo "View Type Validator: Checking findViewById() types against layout view types"
echo "=============================================================================="
echo ""

JAVA_DIR="app/src/main/java/br/jss/motoreviso"
LAYOUT_DIR="app/src/main/res/layout"
ERRORS=0

# Map of Java type patterns to expected XML elements
declare -A TYPE_MAP=(
    ["MaterialButton"]="MaterialButton"
    ["FloatingActionButton"]="FloatingActionButton"
    ["ImageButton"]="ImageButton"
    ["Button"]="Button|MaterialButton"
    ["TextView"]="TextView"
    ["EditText"]="EditText"
    ["ImageView"]="ImageView"
    ["RecyclerView"]="RecyclerView"
    ["ProgressBar"]="ProgressBar"
    ["MapView"]="MapView"
    ["CardView"]="CardView"
    ["TabLayout"]="TabLayout"
)

echo "Checking Java field types against XML element types..."
echo ""

# Extract findViewById calls with their types
for java_file in $(find "$JAVA_DIR" -name "*.java" -exec grep -l "findViewById" {} \;); do
    java_filename=$(basename "$java_file" .java)

    # Infer layout name
    layout_name=$(echo "$java_filename" | sed 's/\([a-z]\)\([A-Z]\)/\1_\2/g' | tr '[:upper:]' '[:lower:]')
    layout_file="$LAYOUT_DIR/$layout_name.xml"

    if [ ! -f "$layout_file" ]; then
        # Try without Activity/Fragment suffix
        layout_name_alt=$(echo "$layout_name" | sed 's/_activity$//' | sed 's/_fragment$//')
        layout_file="$LAYOUT_DIR/$layout_name_alt.xml"
        if [ ! -f "$layout_file" ]; then
            continue
        fi
        layout_name="$layout_name_alt"
    fi

    # Extract all field declarations with findViewById pattern
    # e.g., "private MaterialButton btnX = view.findViewById(R.id.btn_x);"
    # or separate: "private MaterialButton btnX;" and then "btnX = view.findViewById(R.id.btn_x);"

    grep -E "(private|protected|public)\s+\w+\s+\w+.*findViewById\(R\.id\." "$java_file" | while read -r line; do
        # Extract type and ID
        if echo "$line" | grep -qE "private\s+(\w+)\s+(\w+).*findViewById\(R\.id\.(\w+)"; then
            java_type=$(echo "$line" | sed -E 's/.*private\s+(\w+)\s+.*/\1/')
            java_id=$(echo "$line" | sed -E 's/.*R\.id\.(\w+).*/\1/')

            # Find the view in layout with this ID
            xml_element=$(grep -o '<[a-zA-Z.]*' "$layout_file" | grep -B1 "android:id=\"@[+]*id/$java_id\"" | head -1 | sed 's/<//' | sed 's/\..*$//')

            if [ -z "$xml_element" ]; then
                continue
            fi

            # Simple name extraction (e.g., MaterialButton from com.google.android.material.button.MaterialButton)
            xml_simple=$(echo "$xml_element" | awk -F. '{print $NF}')

            # Check if type matches (simple check)
            case "$java_type" in
                Button|MaterialButton)
                    if ! echo "$xml_simple" | grep -qE "Button"; then
                        echo "⚠️  Type mismatch: $java_filename.$java_id"
                        echo "     Java: $java_type, XML: $xml_simple"
                        ((ERRORS++))
                    fi
                    ;;
                FloatingActionButton)
                    if [ "$xml_simple" != "FloatingActionButton" ]; then
                        echo "❌ Type mismatch: $java_filename.$java_id"
                        echo "     Java: $java_type (found in layout: $xml_simple)"
                        ((ERRORS++))
                    fi
                    ;;
                *)
                    if [ "$java_type" != "$xml_simple" ]; then
                        # Allow some flexibility for generic types
                        if ! echo "$xml_simple" | grep -qi "$java_type"; then
                            echo "⚠️  Type mismatch: $java_filename.$java_id"
                            echo "     Java: $java_type, XML: $xml_simple"
                        fi
                    fi
                    ;;
            esac
        fi
    done
done

echo ""
echo "=============================================================================="
if [ $ERRORS -eq 0 ]; then
    echo "✅ No view type mismatches detected!"
else
    echo "❌ Found $ERRORS potential type mismatches"
    echo "   Run the full validator: bash scripts/validate_ids.sh"
fi
echo "=============================================================================="

exit 0
