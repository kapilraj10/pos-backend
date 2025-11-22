#!/bin/bash
# Test script to verify DELETE endpoint with ROLE_ADMIN

echo "=== Testing DELETE endpoint security ==="
echo ""
echo "Please provide:"
echo "1. Your JWT token (with ROLE_ADMIN)"
echo "2. A valid item ID from the database"
echo ""
echo "Then run:"
echo "curl -X DELETE \\"
echo "  http://localhost:8080/api/v1/pos/admin/items/{ITEM_ID} \\"
echo "  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \\"
echo "  -v"
echo ""
echo "Expected responses:"
echo "- 204 No Content = Success (item deleted)"
echo "- 404 Not Found = Item doesn't exist"
echo "- 403 Forbidden = Security issue (should NOT happen with ROLE_ADMIN)"
