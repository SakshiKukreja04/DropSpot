import { auth } from '../config/firebase.js';

/**
 * Middleware to verify Firebase auth token
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

    const token = authHeader.replace(/^Bearer\s+/, '').trim();

    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Malformed authorization header',
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
    console.error('[AUTH] Token verification error:', error.message);

    let message = 'Invalid or expired token';
    let errorType = 'INVALID_TOKEN';

    if (error.code === 'auth/id-token-expired') {
      message = 'Firebase ID token has expired';
      errorType = 'TOKEN_EXPIRED';
    } else if (error.code === 'auth/argument-error') {
      message = 'Invalid token format';
      errorType = 'INVALID_FORMAT';
    }

    return res.status(401).json({
      success: false,
      message: message,
      error: errorType,
      code: 401,
    });
  }
};

/**
 * Global error handling middleware
 */
export const errorHandler = (err, req, res, next) => {
  console.error('[ERROR]', {
    message: err.message,
    code: err.code,
    status: err.status,
  });

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
    path: req.originalUrl,
  });
};
