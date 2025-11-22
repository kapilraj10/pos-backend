# ⚠️ FRONTEND FIX NEEDED

## Problem
Your frontend is getting **403 Forbidden** on DELETE requests because it's **NOT sending the JWT token** in the Authorization header!

```
DELETE http://localhost:8080/api/v1/pos/admin/items/19c078aa-15f6-4c30-8807-b4a7902e02ab 403 (Forbidden)
```

## Root Cause
The backend security is configured correctly, but your React frontend code is **missing the Authorization header** in DELETE requests.

## Fix Your Frontend Code

### Option 1: Fix AppContext.jsx (if using Context API)
```javascript
// In your deleteItem function
const deleteItem = async (itemId) => {
  try {
    const token = localStorage.getItem('token'); // or wherever you store it
    
    await axios.delete(
      `http://localhost:8080/api/v1/pos/admin/items/${itemId}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`  // ⬅️ ADD THIS!
        }
      }
    );
    
    console.log('Item deleted successfully');
  } catch (error) {
    console.error('Delete error:', error);
  }
};
```

### Option 2: Fix Axios Instance (Recommended)
Create an axios instance with default headers:

```javascript
// requests.js or api.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1/pos'
});

// Add token to all requests automatically
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;

// Then use it:
// api.delete(`/admin/items/${itemId}`);
```

### Option 3: Check Existing Axios Configuration
If you already have an axios instance, make sure it includes the token:

```javascript
// Make sure your axios instance looks like this:
const token = localStorage.getItem('token');

axios.delete(url, {
  headers: {
    'Authorization': `Bearer ${token}`,  // ⬅️ THIS IS REQUIRED!
    'Content-Type': 'application/json'
  }
});
```

## How to Verify

### 1. Check Browser DevTools
Open Chrome/Firefox DevTools → Network tab → Click DELETE request → Check Request Headers:

**Should see:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2...
```

**Currently seeing:**
```
(no Authorization header) ❌
```

### 2. Test with curl
```bash
# Get your token first
curl -X POST http://localhost:8080/api/v1/pos/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}'

# Copy the token, then test DELETE
curl -X DELETE \
  http://localhost:8080/api/v1/pos/admin/items/REAL_ITEM_ID \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -v

# Should get 204 No Content (success) or 404 Not Found
# Should NOT get 403 Forbidden
```

### 3. Check Token Storage
Make sure your login is saving the token:

```javascript
// After successful login
localStorage.setItem('token', response.data.token);

// Or if using Context
setToken(response.data.token);
```

## Common Mistakes

### ❌ WRONG - No Authorization header:
```javascript
axios.delete(`http://localhost:8080/api/v1/pos/admin/items/${id}`);
```

### ❌ WRONG - Token not in headers:
```javascript
axios.delete(url, { itemId: id });  // This puts it in body, not headers!
```

### ❌ WRONG - Missing "Bearer " prefix:
```javascript
headers: { Authorization: token }  // Missing "Bearer "
```

### ✅ CORRECT:
```javascript
axios.delete(url, {
  headers: {
    Authorization: `Bearer ${token}`  // ✅ Correct format
  }
});
```

## Backend is Ready ✅

Your Spring Boot backend is already configured correctly:
- ✅ JwtRequestFilter extracts roles from token
- ✅ SecurityConfig accepts ROLE_ADMIN
- ✅ CORS allows DELETE method
- ✅ Controllers have proper @PreAuthorize

The **ONLY** problem is your frontend is not sending the token!

## Next Steps

1. **Find your frontend code:**
   - Look for `AppContext.jsx`, `requests.js`, `api.js`, or similar
   - Find the `deleteItem` or `handleDelete` function

2. **Add Authorization header:**
   - Use one of the fixes above

3. **Test immediately:**
   - Try deleting an item
   - Check browser Network tab
   - Should see Authorization header now

4. **Expected result:**
   - DELETE should return 204 (success) or 404 (item not found)
   - Should NOT return 403 Forbidden

## Still Getting 403?

If you still get 403 after adding the Authorization header:

1. **Check token format:**
   ```javascript
   console.log('Token:', localStorage.getItem('token'));
   // Should start with: eyJhbGciOiJIUzI1NiJ9...
   ```

2. **Check token contains roles:**
   - Login again to get fresh token
   - Token should include {"roles": ["ROLE_ADMIN"]}

3. **Verify user in database:**
   ```sql
   SELECT email, role FROM users WHERE email = 'your_email';
   -- Should show role = 'ADMIN'
   ```

## Summary

🚨 **The backend is working correctly!**  
🚨 **Your frontend needs to send: `Authorization: Bearer <token>`**  
🚨 **Fix your React code to include the token in DELETE requests!**
