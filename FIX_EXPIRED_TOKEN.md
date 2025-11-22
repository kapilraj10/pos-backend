# 🔧 Fix: Token Expired Error

## Problem
```
❌ Token extraction failed: JWT expired at 2025-11-22T11:14:24Z
```

Your JWT token has **expired**. Tokens are now valid for **24 hours**.

## Immediate Solution: Login Again

### Option 1: Quick Fix (Clear Token)
```javascript
// In browser console
localStorage.removeItem('token');
// Then login again in your app
```

### Option 2: Force Logout in Your App
Add a logout button that clears the token:
```javascript
const handleLogout = () => {
  localStorage.removeItem('token');
  // Redirect to login page
  window.location.href = '/login';
};
```

## Backend Changes Made ✅

1. **Increased token expiration: 10 hours → 24 hours**
2. **Better error logging** - Shows when token expired
3. **Response header added** - `X-Token-Expired: true` sent to frontend

## Frontend Fix: Auto-Handle Expired Tokens

### Add Axios Interceptor

Update your axios configuration to automatically handle expired tokens:

```javascript
// In your axios setup (requests.js or api.js)
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1/pos'
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle expired token responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Check if token expired
    if (error.response?.status === 403 || 
        error.response?.headers['x-token-expired'] === 'true') {
      
      console.warn('⚠️ Token expired - redirecting to login');
      
      // Clear expired token
      localStorage.removeItem('token');
      
      // Redirect to login
      window.location.href = '/login';
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

### Update AppContext to Check Token Expiration

```javascript
import { createContext, useState, useEffect } from 'react';

export const AppContext = createContext();

export const AppProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  
  // Check token expiration on mount and periodically
  useEffect(() => {
    const checkTokenExpiration = () => {
      const token = localStorage.getItem('token');
      
      if (!token) return;
      
      try {
        // Decode JWT payload
        const payload = JSON.parse(atob(token.split('.')[1]));
        const expirationTime = payload.exp * 1000; // Convert to milliseconds
        const currentTime = Date.now();
        
        // If expired, clear it
        if (expirationTime < currentTime) {
          console.warn('⚠️ Token expired, clearing...');
          localStorage.removeItem('token');
          setToken(null);
          // Redirect to login
          window.location.href = '/login';
        } else {
          // Token still valid, show time remaining
          const hoursRemaining = Math.floor((expirationTime - currentTime) / (1000 * 60 * 60));
          console.log(`✅ Token valid for ${hoursRemaining} more hours`);
        }
      } catch (error) {
        console.error('Error checking token:', error);
        localStorage.removeItem('token');
        setToken(null);
      }
    };
    
    // Check immediately
    checkTokenExpiration();
    
    // Check every 5 minutes
    const interval = setInterval(checkTokenExpiration, 5 * 60 * 1000);
    
    return () => clearInterval(interval);
  }, []);
  
  const login = async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    const newToken = response.data.token;
    
    localStorage.setItem('token', newToken);
    setToken(newToken);
    
    console.log('✅ Login successful, token expires in 24 hours');
    
    return response.data;
  };
  
  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    window.location.href = '/login';
  };
  
  return (
    <AppContext.Provider value={{ token, login, logout }}>
      {children}
    </AppContext.Provider>
  );
};
```

### Show Token Status in UI (Optional)

```javascript
const TokenStatus = () => {
  const [timeRemaining, setTimeRemaining] = useState('');
  
  useEffect(() => {
    const updateTimeRemaining = () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setTimeRemaining('Not logged in');
        return;
      }
      
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const expirationTime = payload.exp * 1000;
        const currentTime = Date.now();
        const diff = expirationTime - currentTime;
        
        if (diff <= 0) {
          setTimeRemaining('Expired');
          return;
        }
        
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        
        setTimeRemaining(`${hours}h ${minutes}m remaining`);
      } catch (error) {
        setTimeRemaining('Invalid token');
      }
    };
    
    updateTimeRemaining();
    const interval = setInterval(updateTimeRemaining, 60000); // Update every minute
    
    return () => clearInterval(interval);
  }, []);
  
  return (
    <div style={{ fontSize: '12px', color: '#666' }}>
      Session: {timeRemaining}
    </div>
  );
};
```

## Testing

### 1. Clear Old Token
```javascript
// Browser console
localStorage.clear();
```

### 2. Login Again
- Use your login form
- New token will be valid for **24 hours**

### 3. Verify New Token
```javascript
// Browser console
const token = localStorage.getItem('token');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Expires:', new Date(payload.exp * 1000));
```

## Quick Fixes

### Fix 1: Clear and Re-login (Fastest)
```javascript
localStorage.removeItem('token');
// Then login again in your app
```

### Fix 2: Check Token Before Each Request
```javascript
const isTokenValid = () => {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
};

// Before making API calls
if (!isTokenValid()) {
  alert('Your session has expired. Please login again.');
  window.location.href = '/login';
}
```

### Fix 3: Add Login Check on Page Load
```javascript
// In your App.jsx or main component
useEffect(() => {
  const token = localStorage.getItem('token');
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp * 1000 < Date.now()) {
        alert('Your session has expired. Please login again.');
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
    } catch (error) {
      localStorage.removeItem('token');
    }
  }
}, []);
```

## Summary

✅ **Backend fixed:**
- Token expiration increased to 24 hours
- Better error messages
- Response header indicates expired token

🔧 **Frontend needs:**
- Check token expiration on app load
- Add axios interceptor to handle 403 errors
- Auto-redirect to login when token expires
- Clear old token from localStorage

🎯 **Immediate action:**
1. Clear localStorage: `localStorage.clear()`
2. Login again to get fresh token
3. Token will now last 24 hours
4. Add frontend checks to prevent this in future

Now restart your backend and login again!
