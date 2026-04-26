import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import helmet from 'helmet';
import morgan from 'morgan';

// Import routes
import postsRouter from './routes/posts.js';
import requestsRouter from './routes/requests.js';
import savedPostsRouter from './routes/savedPosts.js';
import eventsRouter from './routes/events.js';
import notificationsRouter from './routes/notifications.js';
import usersRouter from './routes/users.js';
import paymentsRouter from './routes/payments.js';
import dispatchRouter from './routes/dispatch.js';

// Import middleware
import { verifyToken, errorHandler, notFoundHandler } from './middleware/auth.js';
import { requestLogger } from './middleware/logging.js';

// Load environment variables
dotenv.config();

// Validate critical environment variables
const requiredEnvVars = [
  'FIREBASE_PROJECT_ID',
  'FIREBASE_PRIVATE_KEY',
  'FIREBASE_CLIENT_EMAIL'
];

for (const envVar of requiredEnvVars) {
  if (!process.env[envVar]) {
    console.error(`❌ Missing required environment variable: ${envVar}`);
    process.exit(1);
  }
}

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 5000;
const NODE_ENV = process.env.NODE_ENV || 'development';

console.log(`🚀 Starting DropSpot Backend - Environment: ${NODE_ENV}, Port: ${PORT}`);

// ============================================
// Security & Logging Middleware
// ============================================

app.use(helmet()); // Security headers
app.use(morgan(NODE_ENV === 'production' ? 'combined' : 'dev'));

// ============================================
// CORS Configuration
// ============================================

const corsOptions = {
  origin: function (origin, callback) {
    // Development mode: allow all
    if (NODE_ENV === 'development') {
      return callback(null, true);
    }

    // Production mode: whitelist specific origins
    const allowedOrigins = [
      'http://localhost:3000',
      'http://localhost:5000',
      'http://10.0.2.2:*',
      'http://192.168.29.133:*',
      process.env.FRONTEND_URL,
      ...(process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : [])
    ].filter(Boolean);

    if (!origin || allowedOrigins.some(allowed =>
      allowed === '*' || new RegExp(allowed.replace(/\*/g, '.*')).test(origin)
    )) {
      callback(null, true);
    } else {
      console.warn(`[CORS] Blocked origin: ${origin}`);
      callback(new Error('CORS not allowed'));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  maxAge: 86400,
};

app.use(cors(corsOptions));

// ============================================
// Body Parser Middleware
// ============================================

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// ============================================
// Custom Request Logging
// ============================================

app.use(requestLogger);

// ============================================
// Health Check Endpoints (Public - No Auth)
// ============================================

app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'DropSpot API Server is running',
    environment: NODE_ENV,
    version: '1.0.0',
    timestamp: new Date().toISOString(),
  });
});

app.get('/health', (req, res) => {
  res.status(200).json({
    success: true,
    status: 'healthy',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
  });
});

// ============================================
// API Routes (Protected with Auth Middleware)
// ============================================

app.use('/api', verifyToken);

app.use('/api/posts', postsRouter);
app.use('/api/requests', requestsRouter);
app.use('/api/saved', savedPostsRouter);
app.use('/api/events', eventsRouter);
app.use('/api/notifications', notificationsRouter);
app.use('/api/users', usersRouter);
app.use('/api/payments', paymentsRouter);
app.use('/api/dispatch', dispatchRouter);
-
// ============================================
// Error Handling Middleware
// ============================================

app.use(notFoundHandler);
app.use(errorHandler);

// ============================================
// Server Startup
// ============================================

const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Server listening on port ${PORT}`);
  console.log(`🌐 Environment: ${NODE_ENV}`);
  console.log(`📅 Started at: ${new Date().toISOString()}`);
});

// ============================================
// Graceful Shutdown Handling
// ============================================

process.on('SIGTERM', () => {
  console.log('⚠️  SIGTERM received - graceful shutdown initiated');
  server.close(() => {
    console.log('✅ Server closed');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('⚠️  SIGINT received - graceful shutdown initiated');
  server.close(() => {
    console.log('✅ Server closed');
    process.exit(0);
  });
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('❌ Unhandled Rejection:', reason);
});

process.on('uncaughtException', (error) => {
  console.error('❌ Uncaught Exception:', error);
  process.exit(1);
});

export default app;

