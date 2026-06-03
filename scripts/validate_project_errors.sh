#!/bin/bash

#############################################################################
# Project Error Validator
# Checks for common compilation and runtime errors in the MotoReviso project
#############################################################################

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$APP_DIR/app/src/main/java"
LAYOUT_DIR="$APP_DIR/app/src/main/res/layout"
DRAWABLE_DIR="$APP_DIR/app/src/main/res/drawable"
COLOR_DIR="$APP_DIR/app/src/main/res/color"
VALUES_DIR="$APP_DIR/app/src/main/res/values"

ERRORS=()
WARNINGS=()

echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║              🔍 PROJECT ERROR VALIDATION 🔍                              ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"

# ============================================================================
# 1. Check all layout IDs are defined (Core validation)
# ============================================================================
echo -e "\n${BLUE}[1/7] Checking layout IDs...${NC}"

bash "$APP_DIR/scripts/validate_ids.sh" > /tmp/id_validation.log 2>&1
if grep -q "NOT FOUND" /tmp/id_validation.log; then
    while IFS= read -r line; do
        if [[ $line == *"NOT FOUND"* ]]; then
            ERRORS+=("$line")
        fi
    done < /tmp/id_validation.log
fi

if [ ${#ERRORS[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ All layout IDs are correctly defined${NC}"
else
    echo -e "${RED}❌ ID validation failed:${NC}"
    printf '%s\n' "${ERRORS[@]}"
fi

# ============================================================================
# 2. Check View type mismatches
# ============================================================================
echo -e "\n${BLUE}[2/7] Checking View types...${NC}"

bash "$APP_DIR/scripts/validate_view_types.sh" > /tmp/type_validation.log 2>&1
if grep -q "Mismatch\|cannot be cast" /tmp/type_validation.log; then
    while IFS= read -r line; do
        if [[ $line == *"Mismatch"* ]] || [[ $line == *"cannot be cast"* ]]; then
            ERRORS+=("$line")
        fi
    done < /tmp/type_validation.log
fi

if [ ${#ERRORS[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ No View type mismatches detected${NC}"
fi

# ============================================================================
# 3. Check for undefined callbacks used in Java
# ============================================================================
echo -e "\n${BLUE}[3/7] Checking callback interface definitions...${NC}"

# Check if VeiculoCallback is defined
if grep -q "interface VeiculoCallback" "$JAVA_DIR/br/jss/motoreviso/managers/FirebaseManager.java" 2>/dev/null; then
    echo -e "${GREEN}✅ VeiculoCallback interface is defined${NC}"
else
    ERRORS+=("❌ VeiculoCallback interface not found in FirebaseManager")
    echo -e "${RED}❌ VeiculoCallback interface not found${NC}"
fi

# Check if TrajetosCallback is defined
if grep -q "interface TrajetosCallback" "$JAVA_DIR/br/jss/motoreviso/managers/FirebaseManager.java" 2>/dev/null; then
    echo -e "${GREEN}✅ TrajetosCallback interface is defined${NC}"
else
    ERRORS+=("❌ TrajetosCallback interface not found in FirebaseManager")
    echo -e "${RED}❌ TrajetosCallback interface not found${NC}"
fi

# ============================================================================
# 4. Check for missing drawable resources
# ============================================================================
echo -e "\n${BLUE}[4/7] Checking drawable resources...${NC}"

MISSING_DRAWABLES=0
DRAWABLE_REFS=$(grep -rho '@drawable/[a-z_0-9]*' "$LAYOUT_DIR" 2>/dev/null | sed 's/@drawable\///' | sort -u)

for drawable in $DRAWABLE_REFS; do
    if [ ! -f "$DRAWABLE_DIR/${drawable}.xml" ] 2>/dev/null; then
        WARNINGS+=("⚠️  Missing drawable: @drawable/$drawable")
        ((MISSING_DRAWABLES++))
    fi
done

if [ $MISSING_DRAWABLES -eq 0 ]; then
    echo -e "${GREEN}✅ All drawable resources exist${NC}"
else
    echo -e "${YELLOW}⚠️  $MISSING_DRAWABLES missing drawable resource(s)${NC}"
    printf '%s\n' "${WARNINGS[@]:${#WARNINGS[@]}-$MISSING_DRAWABLES}"
fi

# ============================================================================
# 5. Check for undefined color resources
# ============================================================================
echo -e "\n${BLUE}[5/7] Checking color resources...${NC}"

MISSING_COLORS=0

# Check colors.xml
if grep -q 'name="nav_item_color"' "$VALUES_DIR/colors.xml" 2>/dev/null; then
    echo -e "${GREEN}✅ nav_item_color defined in colors.xml${NC}"
elif [ -f "$COLOR_DIR/nav_item_color.xml" ] 2>/dev/null; then
    echo -e "${GREEN}✅ nav_item_color defined as color state list${NC}"
else
    ERRORS+=("❌ nav_item_color not defined (checked colors.xml and $COLOR_DIR)")
    echo -e "${RED}❌ nav_item_color not defined${NC}"
    ((MISSING_COLORS++))
fi

# ============================================================================
# 6. Check Material Design 2 compatibility
# ============================================================================
echo -e "\n${BLUE}[6/7] Checking Material Design 2 compatibility...${NC}"

M3_COUNT=$(grep -r "itemActiveIndicatorColor\|shapeAppearanceOverride\|itemShapeAppearance" "$LAYOUT_DIR" 2>/dev/null | wc -l)

if [ $M3_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ No M3-only attributes found${NC}"
else
    ERRORS+=("❌ Found $M3_COUNT M3-only attribute(s)")
    echo -e "${RED}❌ Found $M3_COUNT M3-only attribute(s)${NC}"
    grep -rn "itemActiveIndicatorColor\|shapeAppearanceOverride\|itemShapeAppearance" "$LAYOUT_DIR" 2>/dev/null | head -5
fi

# ============================================================================
# 7. Check FirebaseManager methods exist
# ============================================================================
echo -e "\n${BLUE}[7/7] Checking FirebaseManager methods...${NC}"

REQUIRED_METHODS=(
    "carregarVeiculoPrincipal"
    "carregarTrajetos"
    "obterTodosVeiculos"
    "obterTrajetosUsuario"
)

MISSING_METHODS=0
for method in "${REQUIRED_METHODS[@]}"; do
    if grep -q "public.*$method(" "$JAVA_DIR/br/jss/motoreviso/managers/FirebaseManager.java" 2>/dev/null; then
        echo -e "  ${GREEN}✅${NC} $method"
    else
        ERRORS+=("❌ FirebaseManager.$method() not defined")
        echo -e "  ${RED}❌${NC} $method"
        ((MISSING_METHODS++))
    fi
done

# ============================================================================
# SUMMARY
# ============================================================================
echo ""
echo "╔════════════════════════════════════════════════════════════════════════════╗"

TOTAL_ERRORS=${#ERRORS[@]}
TOTAL_WARNINGS=${#WARNINGS[@]}

if [ $TOTAL_ERRORS -eq 0 ]; then
    if [ $TOTAL_WARNINGS -eq 0 ]; then
        echo -e "║                   ${GREEN}✅ NO ISSUES DETECTED - READY TO BUILD!${NC}                     ║"
        EXIT_CODE=0
    else
        echo -e "║              ${YELLOW}⚠️  $TOTAL_WARNINGS WARNING(S) - BUILD MAY WORK${NC}                   ║"
        EXIT_CODE=0
    fi
else
    echo -e "║                ${RED}❌ $TOTAL_ERRORS CRITICAL ERROR(S) - FIX BEFORE BUILD${NC}                  ║"
    EXIT_CODE=1
fi

echo "╚════════════════════════════════════════════════════════════════════════════╝"

# Print error details
if [ $TOTAL_ERRORS -gt 0 ]; then
    echo -e "\n${RED}Critical Errors:${NC}"
    printf '%s\n' "${ERRORS[@]}"
fi

if [ $TOTAL_WARNINGS -gt 0 ]; then
    echo -e "\n${YELLOW}Warnings:${NC}"
    printf '%s\n' "${WARNINGS[@]}"
fi

exit $EXIT_CODE
