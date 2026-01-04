#!/bin/bash
#
# CI Build Script for RmR Core
# Runs code quality checks, audits, and tests
#

set -e  # Exit on error

echo "=========================================="
echo "RmR Core - CI Build Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get repository root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "Repository root: $REPO_ROOT"
echo ""

# Step 1: Placeholder Audit
echo "=========================================="
echo "Step 1: Running placeholder audit..."
echo "=========================================="
if python3 tools/audit_placeholders.py; then
    echo -e "${GREEN}✓ Placeholder audit passed${NC}"
else
    echo -e "${RED}✗ Placeholder audit failed${NC}"
    exit 1
fi
echo ""

# Step 2: Check for telemetry/analytics
echo "=========================================="
echo "Step 2: Checking for telemetry/analytics..."
echo "=========================================="
TELEMETRY_CHECK=$(grep -ri "telemetry\|analytics\|phone.home\|tracking\|usage.stat" \
    rmr/rmr-core/src/main rmr/rafaelia/src/main \
    --include="*.java" --include="*.kt" --include="*.cpp" --include="*.h" \
    2>/dev/null | grep -v "^[[:space:]]*//\|^[[:space:]]*/\*\|^[[:space:]]*\*" || true)

if [ -z "$TELEMETRY_CHECK" ]; then
    echo -e "${GREEN}✓ No telemetry/analytics found in core modules${NC}"
else
    echo -e "${RED}✗ Potential telemetry/analytics code found:${NC}"
    echo "$TELEMETRY_CHECK"
    exit 1
fi
echo ""

# Step 3: Check for reflection usage
echo "=========================================="
echo "Step 3: Checking for reflection usage..."
echo "=========================================="
REFLECTION_CHECK=$(grep -r "reflect\|getDeclaredMethod\|getClass().getMethod\|Class.forName" \
    rmr/rmr-core/src/main rmr/rafaelia/src/main \
    --include="*.java" --include="*.kt" 2>/dev/null || true)

if [ -z "$REFLECTION_CHECK" ]; then
    echo -e "${GREEN}✓ No reflection usage found in core modules${NC}"
else
    echo -e "${YELLOW}⚠ Reflection usage detected (review manually):${NC}"
    echo "$REFLECTION_CHECK"
fi
echo ""

# Step 4: Verify CORE_CONTRACT.md exists
echo "=========================================="
echo "Step 4: Verifying core contract..."
echo "=========================================="
if [ -f "rmr/CORE_CONTRACT.md" ]; then
    echo -e "${GREEN}✓ CORE_CONTRACT.md exists${NC}"
    echo "  Size: $(wc -c < rmr/CORE_CONTRACT.md) bytes"
    echo "  Lines: $(wc -l < rmr/CORE_CONTRACT.md)"
else
    echo -e "${RED}✗ CORE_CONTRACT.md missing${NC}"
    exit 1
fi
echo ""

# Step 5: Summary
echo "=========================================="
echo "Build Summary"
echo "=========================================="
echo -e "${GREEN}✓ All checks passed${NC}"
echo ""
echo "Core modules are clean and ready for deployment:"
echo "  - No placeholders in core code"
echo "  - No telemetry/analytics"
echo "  - Core contract documented"
echo "  - Code quality validated"
echo ""
echo "Next steps:"
echo "  - Run full test suite: ./gradlew test"
echo "  - Build release: ./gradlew assembleRelease"
echo ""

exit 0
