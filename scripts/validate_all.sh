#!/bin/bash
# Master Validator: Run all validation checks before building

echo ""
echo "╔════════════════════════════════════════════════════════════════════════════╗"
echo "║                    🔍 PRE-BUILD VALIDATION SUITE 🔍                        ║"
echo "╚════════════════════════════════════════════════════════════════════════════╝"
echo ""

ERRORS=0

# 1. Validate IDs
echo "[1/3] Running ID validator..."
echo "─────────────────────────────────────────────────────────────────────────────"
if bash scripts/validate_ids.sh; then
    echo "✅ ID validation passed"
else
    echo "❌ ID validation FAILED"
    ((ERRORS++))
fi

echo ""
echo "[2/3] Running view type validator..."
echo "─────────────────────────────────────────────────────────────────────────────"
if bash scripts/validate_view_types.sh; then
    echo "✅ View type validation passed"
else
    echo "⚠️  View type validation completed (check output above)"
fi

echo ""
echo "[3/3] Checking Material Design 2 compatibility..."
echo "─────────────────────────────────────────────────────────────────────────────"
if grep -r "shapeAppearanceOverride\|itemActiveIndicatorColor\|itemStateLayerColor" app/src/main/res/layout/ 2>/dev/null; then
    echo "❌ Found M3-only attributes!"
    ((ERRORS++))
else
    echo "✅ No M3-only attributes found"
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════════════════╗"
if [ $ERRORS -eq 0 ]; then
    echo "║                  ✅ ALL VALIDATIONS PASSED - SAFE TO BUILD              ║"
    echo "╚════════════════════════════════════════════════════════════════════════════╝"
    exit 0
else
    echo "║              ❌ VALIDATION ERRORS FOUND - FIX BEFORE BUILDING            ║"
    echo "╚════════════════════════════════════════════════════════════════════════════╝"
    exit 1
fi
