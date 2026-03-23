import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

// Import routes
import postsRouter from './routes/posts.js';
import requestsRouter from './routes/requests.js';
import savedPostsRouter from './routes/savedPosts.js';
import eventsRouter from './routes/events.js';
import notificationsRouter from './routes/notifications.js';
import usersRouter from './routes/users.js';

// Import middleware
import { verifyToken, errorHandler, notFoundHandler } from './middleware/auth.js';

// Load environment variables
dotenv.config();

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 5000;
const NODE_ENV = process.env.NODE_ENV || 'development';

// ============================================
// Middleware Configuration
// ============================================

// CORS configuration
const corsOptions = {
  origin: [
    'http://localhost:3000',
    'http://localhost:5000',
    'http://localhost:8080',
    process.env.FRONTEND_URL,
  ].filter(Boolean),
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
};

app.use(cors(corsOptions));

// Body parser middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// Request logging middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] ${req.method} ${req.path}`);
  next();
});

// ============================================
// Health Check Endpoint
// ============================================

/**
 * GET / - Health check endpoint
 */
app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'DropSpot API Server is running',
    version: '1.0.0',
    environment: NODE_ENV,
    timestamp: new Date().toISOString(),
  });
});

/**
 * GET /health - Another health check endpoint
 */
app.get('/health', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'API is healthy',
    timestamp: new Date().toISOString(),
  });
});

// ============================================
// Authentication Verification
// ============================================

// Apply token verification to all API routes (except health and root)
app.use('/api', verifyToken);

// ============================================
// API Routes
// ============================================

/**
 * Public routes (no auth required)
 */

/**
 * Protected routes (auth required)
 */

// Posts routes
app.use('/api/posts', postsRouter);

// Requests routes
app.use('/api/requests', requestsRouter);

// Saved posts routes
app.use('/api/saved', savedPostsRouter);

// Events routes
app.use('/api/events', eventsRouter);

// Notifications routes
app.use('/api/notifications', notificationsRouter);

// Users routes
app.use('/api/users', usersRouter);

// ============================================
// Error Handling
// ============================================

// 404 handler for undefined routes
app.use(notFoundHandler);

// Global error handler
app.use(errorHandler);

// ============================================
// Server Startup
// ============================================

const server = app.listen(PORT, () => {
  console.log(`
═══════════════════════════════════════════════════════════
  DropSpot API Backend Server
═══════════════════════════════════════════════════════════
  Environment: ${NODE_ENV}
  Port: ${PORT}
  Started: ${new Date().toISOString()}
═══════════════════════════════════════════════════════════
  
  Available Endpoints:
  
  POSTS:
    POST   /api/posts
    GET    /api/posts
    GET    /api/posts/:id
    PUT    /api/posts/:id
    DELETE /api/posts/:id
    PUT    /api/posts/:id/view
  
  REQUESTS:
    POST   /api/requests
    GET    /api/requests
    GET    /api/requests/:id
    PUT    /api/requests/:id
    DELETE /api/requests/:id
  
  SAVED POSTS:
    POST   /api/saved
    GET    /api/saved/:userId
    DELETE /api/saved/:postId
    GET    /api/saved/check/:postId
  
  EVENTS:
    POST   /api/events
    GET    /api/events
    GET    /api/events/:id
    PUT    /api/events/:id
    DELETE /api/events/:id
    POST   /api/events/:id/join
    DELETE /api/events/:id/leave
  
  NOTIFICATIONS:
    GET    /api/notifications/:userId
    PUT    /api/notifications/:id
    PUT    /api/notifications/batch/read
    DELETE /api/notifications/:id
    DELETE /api/notifications/batch/delete
    GET    /api/notifications/:userId/unread-count
  
  USERS:
    POST   /api/users
    GET    /api/users/:userId
    GET    /api/users/:userId/posts
    GET    /api/users/:userId/stats
    PUT    /api/users/:userId/rating
  
═══════════════════════════════════════════════════════════
  `);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM signal received: closing HTTP server');
  server.close(() => {
    console.log('HTTP server closed');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('SIGINT signal received: closing HTTP server');
  server.close(() => {
    console.log('HTTP server closed');
    process.exit(0);
  });
});

// Unhandled rejection handler
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

export default app;
