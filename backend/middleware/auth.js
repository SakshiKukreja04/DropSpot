import { auth } from '../config/firebase.js';

/**
 * Middleware to verify Firebase auth token
 * Verifies the Authorization header and attaches user info to request
 */
export const verifyToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No authorization token provided',
        error: 'UNAUTHORIZED',
        code: 401,
      });
    }

    // Extract token more robustly handling multiple spaces and trimming
    const token = authHeader.replace(/^Bearer\s+/, '').trim();

    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Malformed authorization header: Token missing',
        error: 'INVALID_TOKEN_FORMAT',
        code: 401,
      });
    }

    // Verify the token with Firebase
    const decodedToken = await auth.verifyIdToken(token);
    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email,
      name: decodedToken.name,
      picture: decodedToken.picture,
    };

    next();
  } catch (error) {
    console.error('Token verification error:', error.message);

    let message = 'Invalid or expired token';
    let errorType = 'INVALID_TOKEN';

    if (error.code === 'auth/id-token-expired') {
      message = 'Firebase ID token has expired';
      errorType = 'TOKEN_EXPIRED';
    } else if (error.code === 'auth/argument-error') {
      message = 'Invalid token format or project mismatch';
      errorType = 'ARGUMENT_ERROR';
    }

    return res.status(401).json({
      success: false,
      message: message,
      error: errorType,
      code: 401,
      details: error.message
    });
  }
};

/**
 * Global error handling middleware
 */
export const errorHandler = (err, req, res, next) => {
  console.error('Error:', err);

  const status = err.status || 500;
  const message = err.message || 'Internal Server Error';

  res.status(status).json({
    success: false,
    message,
    error: err.error || 'SERVER_ERROR',
    code: status,
  });
};

/**
 * 404 handler middleware
 */
export const notFoundHandler = (req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found',
    error: 'NOT_FOUND',
    code: 404,
  });
};
