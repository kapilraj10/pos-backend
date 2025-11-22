# 🔧 Token Middleware Debug & Fix Guide

## Current Setup Analysis

### Your Frontend Code (Correct ✅)
```javascript
// addItem - Sending token correctly
export const addItem = async (formData) => {
  return await axios.post(
    "http://localhost:8080/api/v1/pos/admin/items",
    formData,
    { headers: { 'Authorization': `Bearer ${localStorage.getItem("token")}` } }
  );
};

// deleteItem - Sending token correctly
export const deleteItem = async (itemId) => {
  return await axios.delete(
    `http://localhost:8080/api/v1/pos/admin/items/${itemId}`,
    { headers: { Authorization: `Bearer ${localStorage.getItem("token")}` } }
  );
};
```

## Enhanced Backend Logging ✅

I've added detailed logging to track token processing. Now run your app and watch the logs:

```bash
./mvnw spring-boot:run
```

### What You'll See in Logs:

**When Token is Working:**
```
🔍 POST /api/v1/pos/admin/items | Auth Header: Present ✅ | Content-Type: multipart/form-data
✅ Token valid for user: your@email.com
✅ Authorities from JWT: [ROLE_ADMIN]
✅ Authentication set successfully for: your@email.com
```

**When Token is Missing:**
```
🔍 POST /api/v1/pos/admin/items | Auth Header: MISSING ❌ | Content-Type: multipart/form-data
❌ Admin endpoint accessed without Bearer token: /api/v1/pos/admin/items
```

**When Token is Invalid:**
```
🔍 POST /api/v1/pos/admin/items | Auth Header: Present ✅ | Content-Type: multipart/form-data
❌ Token extraction failed: JWT expired
```

## Common Issues & Fixes

### Issue 1: Token in localStorage is NULL

**Check:**
```javascript
console.log('Token:', localStorage.getItem('token'));
```

**If null, fix login:**
```javascript
// In your login function
const login = async (credentials) => {
  const response = await axios.post(
    'http://localhost:8080/api/v1/pos/auth/login',
    credentials
  );
  
  // ✅ Save token after successful login
  localStorage.setItem('token', response.data.token);
  
  return response.data;
};
```

### Issue 2: Token Expired

**Symptoms:** Works initially, then stops working after 30+ minutes

**Check token expiration:**
```javascript
// Decode JWT to see expiration (client-side check)
const token = localStorage.getItem('token');
if (token) {
  const payload = JSON.parse(atob(token.split('.')[1]));
  console.log('Token expires:', new Date(payload.exp * 1000));
  console.log('Is expired?', payload.exp * 1000 < Date.now());
}
```

**Fix:** Login again to get fresh token

### Issue 3: Wrong Token Format

**Check the token value:**
```javascript
const token = localStorage.getItem('token');
console.log('Token starts with:', token?.substring(0, 20));
// Should start with: eyJhbGciOiJIUzI1NiJ9
// Should NOT include "Bearer " prefix (that's added by axios)
```

**Correct storage:**
```javascript
// ❌ WRONG - Don't store "Bearer " prefix
localStorage.setItem('token', 'Bearer ' + response.data.token);

// ✅ CORRECT - Store just the token
localStorage.setItem('token', response.data.token);
```

### Issue 4: CORS Preflight Blocking Token

**Symptom:** OPTIONS request succeeds, but POST/DELETE fails

**Check browser console:** Look for CORS errors

**Backend already configured correctly:**
```java
config.setAllowedHeaders(List.of("*"));  // Allows Authorization header
config.setAllowCredentials(true);         // Required for auth
```

### Issue 5: Token Not in Request Headers

**Browser DevTools Check:**
1. Open DevTools → Network tab
2. Try to add/delete an item
3. Click the POST/DELETE request
4. Check **Request Headers** section

**Should see:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ5b3VyQGVtYWlsLmNvbSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNzAwNjQ5NjAwLCJleHAiOjE3MDA2NTMyMDB9...
```

**If missing:** Check that `localStorage.getItem('token')` returns a value

## Test Flow

### 1. Get Fresh Token
```bash
curl -X POST http://localhost:8080/api/v1/pos/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_email@example.com","password":"your_password"}' \
  | jq '.token'
```

Copy the token value (without quotes).

### 2. Test Add Item (with file)
```bash
TOKEN="paste_your_token_here"

curl -X POST http://localhost:8080/api/v1/pos/admin/items \
  -H "Authorization: Bearer $TOKEN" \
  -F 'item={"name":"Test Item","categoryName":"Test Category","price":100,"description":"Test"}' \
  -F 'image=@/path/to/image.jpg' \
  -v
```

### 3. Test Delete Item
```bash
TOKEN="paste_your_token_here"
ITEM_ID="get_from_items_list"

curl -X DELETE http://localhost:8080/api/v1/pos/admin/items/$ITEM_ID \
  -H "Authorization: Bearer $TOKEN" \
  -v
```

### Expected Responses:
- **200 OK** (POST add item) = Success
- **204 No Content** (DELETE) = Success
- **403 Forbidden** = Token issue
- **401 Unauthorized** = No token or invalid

## Frontend Debug Wrapper

Add this to temporarily see what's happening:

```javascript
// Wrap your API calls with debug logging
const debugAPI = async (apiCall, label) => {
  const token = localStorage.getItem('token');
  console.log(`🔍 ${label}`);
  console.log('  Token exists:', !!token);
  console.log('  Token preview:', token?.substring(0, 30) + '...');
  
  try {
    const response = await apiCall();
    console.log('  ✅ Success:', response.status);
    return response;
  } catch (error) {
    console.error('  ❌ Failed:', error.response?.status, error.response?.data);
    throw error;
  }
};

// Use it:
await debugAPI(() => addItem(formData), 'ADD ITEM');
await debugAPI(() => deleteItem(itemId), 'DELETE ITEM');
```

## Quick Fixes

### Fix 1: Ensure Token on Login
```javascript
const handleLogin = async (credentials) => {
  try {
    const response = await axios.post('/auth/login', credentials);
    
    // ✅ Critical: Save token
    localStorage.setItem('token', response.data.token);
    
    // ✅ Verify it's saved
    console.log('Token saved:', !!localStorage.getItem('token'));
    
  } catch (error) {
    console.error('Login failed:', error);
  }
};
```

### Fix 2: Global Axios Default
```javascript
// Set default header for all requests (alternative approach)
import axios from 'axios';

// Get token and set as default header
const token = localStorage.getItem('token');
if (token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}
```

### Fix 3: Axios Interceptor (Best Practice)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1/pos'
});

// Automatically add token to every request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('✅ Token added to request:', config.url);
    } else {
      console.warn('⚠️ No token found for request:', config.url);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Use api instead of axios
export const addItem = async (formData) => {
  return await api.post('/admin/items', formData);
};

export const deleteItem = async (itemId) => {
  return await api.delete(`/admin/items/${itemId}`);
};
```

## Test Checklist

Run through this checklist:

- [ ] Login successfully
- [ ] Check `localStorage.getItem('token')` returns a value
- [ ] Token starts with `eyJ` (JWT format)
- [ ] Token doesn't include "Bearer " prefix in storage
- [ ] Browser DevTools shows Authorization header in requests
- [ ] Backend logs show "Auth Header: Present ✅"
- [ ] Backend logs show "✅ Token valid for user: email"
- [ ] Backend logs show "✅ Authorities from JWT: [ROLE_ADMIN]"
- [ ] POST/DELETE returns 200/204, not 403

## Still Not Working?

If you still get 403 after all checks:

1. **Clear everything:**
   ```javascript
   localStorage.clear();
   // Login again
   ```

2. **Check database:**
   ```sql
   SELECT email, role FROM users WHERE email = 'your_email';
   -- Should show: role = 'ADMIN' (not 'ROLE_ADMIN')
   ```

3. **Check JWT generation:**
   - Login should return token with `{"roles": ["ROLE_ADMIN"]}`
   - Backend JwtUtil should be adding roles to token

4. **Watch backend logs:**
   - Run `./mvnw spring-boot:run`
   - Try add/delete
   - Look for error messages in red

## Success Indicators

✅ **Working correctly when you see:**
- Token in localStorage
- Authorization header in Network tab
- Backend logs: "Present ✅"
- Backend logs: "✅ Token valid"
- Backend logs: "✅ Authorities from JWT: [ROLE_ADMIN]"
- POST returns 200, DELETE returns 204

Now test your add and delete operations!
