#!/bin/bash

#############################################################################
# Android Studio Common Errors Detector
# Finds and reports common Java/Android errors that Android Studio would flag
#############################################################################

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$APP_DIR/app/src/main/java"
MODELS_DIR="$JAVA_DIR/br/jss/motoreviso/models"

ERRORS=()
WARNINGS=()

echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║         🔍 ANDROID STUDIO COMMON ERRORS DETECTOR 🔍                       ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"

# ============================================================================
# 1. Check for implicit casts (View found as different type than declared)
# ============================================================================
echo -e "\n${BLUE}[1/5] Checking for implicit cast issues...${NC}"

# Find all findViewById calls with MaterialCardView
CAST_COUNT=$(grep -rn "MaterialCardView.*findViewById\|findViewById.*MaterialCardView" "$JAVA_DIR" 2>/dev/null | wc -l)

# Check for type declaration mismatches
if grep -q "MaterialCardView cardStats.*LinearLayout\|LinearLayout.*cardStats.*MaterialCardView" "$JAVA_DIR" 2>/dev/null; then
    WARNINGS+=("⚠️  Implicit cast issue found - check cardStats declaration")
    echo -e "${YELLOW}⚠️  Found potential implicit cast issues (cardStats)${NC}"
else
    echo -e "${GREEN}✅ No obvious implicit cast issues${NC}"
fi

# ============================================================================
# 2. Check for undefined method calls on model objects
# ============================================================================
echo -e "\n${BLUE}[2/5] Checking for undefined method calls...${NC}"

UNDEFINED_METHODS=0

# Check Trajeto methods
if grep -rn "trajeto\.getNome()" "$JAVA_DIR" 2>/dev/null; then
    ERRORS+=("❌ Trajeto.getNome() does not exist - use getOrigem() or getDestino()")
    echo -e "${RED}❌ Found: trajeto.getNome() - method doesn't exist${NC}"
    grep -rn "trajeto\.getNome()" "$JAVA_DIR" | head -2
    ((UNDEFINED_METHODS++))
fi

if grep -rn "trajeto\.getDistancia()" "$JAVA_DIR" 2>/dev/null; then
    ERRORS+=("❌ Trajeto.getDistancia() does not exist - use getKmRodados()")
    echo -e "${RED}❌ Found: trajeto.getDistancia() - method doesn't exist${NC}"
    grep -rn "trajeto\.getDistancia()" "$JAVA_DIR" | head -2
    ((UNDEFINED_METHODS++))
fi

# Check Veiculo methods
if grep -rn "veiculo\.getNome()" "$JAVA_DIR" 2>/dev/null; then
    ERRORS+=("❌ Veiculo.getNome() - check class for correct method")
    echo -e "${RED}❌ Found: veiculo.getNome() - verify method exists${NC}"
    ((UNDEFINED_METHODS++))
fi

if [ $UNDEFINED_METHODS -eq 0 ]; then
    echo -e "${GREEN}✅ No undefined method calls detected${NC}"
fi

# ============================================================================
# 3. Check for API level incompatibilities
# ============================================================================
echo -e "\n${BLUE}[3/5] Checking for API level incompatibilities...${NC}"

API_ISSUES=0

# Calendar.Builder requires API 26
if grep -rn "new.*Calendar\.Builder()" "$JAVA_DIR" 2>/dev/null; then
    WARNINGS+=("⚠️  Calendar.Builder() requires API 26 (min is 24) - use Calendar.getInstance()")
    echo -e "${YELLOW}⚠️  Found Calendar.Builder() - requires API 26${NC}"
    grep -rn "new.*Calendar\.Builder()" "$JAVA_DIR" | head -2
    ((API_ISSUES++))
fi

# LocalDate requires API 26
if grep -rn "LocalDate\|LocalDateTime\|LocalTime" "$JAVA_DIR" 2>/dev/null; then
    WARNINGS+=("⚠️  Java.time API requires API 26 (min is 24)")
    ((API_ISSUES++))
fi

# TextUtils.isDigitsOnly requires API 21 (OK)
# TextUtils.isEmpty is OK (API 1)

if [ $API_ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ No API level incompatibilities found${NC}"
fi

# ============================================================================
# 4. Check for null pointer risks
# ============================================================================
echo -e "\n${BLUE}[4/5] Checking for null pointer risks...${NC}"

NULL_RISKS=0

# Check for direct method calls without null checks
if grep -rn "trajeto\.getKmRodados()" "$JAVA_DIR" | grep -v "!= null\|null ?\|if.*!= null"; then
    WARNINGS+=("⚠️  getKmRodados() may return null - add null check")
    echo -e "${YELLOW}⚠️  Found potential null pointer risks (getKmRodados, etc)${NC}"
    ((NULL_RISKS++))
fi

if [ $NULL_RISKS -eq 0 ]; then
    echo -e "${GREEN}✅ No obvious null pointer risks${NC}"
fi

# ============================================================================
# 5. Check for deprecated API usage
# ============================================================================
echo -e "\n${BLUE}[5/5] Checking for deprecated API usage...${NC}"

DEPRECATED_COUNT=0

# Check for deprecated methods
if grep -rn "@Deprecated" "$JAVA_DIR" 2>/dev/null | grep -v "comment\|//"; then
    # Count files using deprecated methods
    DEPRECATED_COUNT=$(grep -r "obterTodasManutemcoes()" "$JAVA_DIR" 2>/dev/null | wc -l)
    if [ $DEPRECATED_COUNT -gt 0 ]; then
        WARNINGS+=("⚠️  Found $DEPRECATED_COUNT usage(s) of deprecated obterTodasManutemcoes() - use obterTodasManutencoes()")
        echo -e "${YELLOW}⚠️  Found deprecated method calls${NC}"
    fi
fi

if [ $DEPRECATED_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ No obvious deprecated API usage${NC}"
fi

# ============================================================================
# SUMMARY
# ============================================================================
echo ""
echo "╔════════════════════════════════════════════════════════════════════════════╗"

TOTAL_ERRORS=${#ERRORS[@]}
TOTAL_WARNINGS=${#WARNINGS[@]}

if [ $TOTAL_ERRORS -eq 0 ] && [ $TOTAL_WARNINGS -eq 0 ]; then
    echo -e "║                   ${GREEN}✅ NO ISSUES DETECTED${NC}                                  ║"
    EXIT_CODE=0
elif [ $TOTAL_ERRORS -eq 0 ]; then
    echo -e "║              ${YELLOW}⚠️  $TOTAL_WARNINGS WARNING(S) - REVIEW RECOMMENDED${NC}              ║"
    EXIT_CODE=0
else
    echo -e "║                ${RED}❌ $TOTAL_ERRORS ERROR(S) - FIX BEFORE BUILD${NC}                   ║"
    EXIT_CODE=1
fi

echo "╚════════════════════════════════════════════════════════════════════════════╝"

# Print details
if [ $TOTAL_ERRORS -gt 0 ]; then
    echo -e "\n${RED}Critical Errors:${NC}"
    printf '%s\n' "${ERRORS[@]}"
fi

if [ $TOTAL_WARNINGS -gt 0 ]; then
    echo -e "\n${YELLOW}Warnings:${NC}"
    printf '%s\n' "${WARNINGS[@]}"
fi

exit $EXIT_CODE
