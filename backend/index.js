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
import paymentsRouter from './routes/payments.js';
import dispatchRouter from './routes/dispatch.js';

// Import middleware
import { verifyToken, errorHandler, notFoundHandler } from './middleware/auth.js';

// Load environment variables
dotenv.config();

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 4000;
const NODE_ENV = process.env.NODE_ENV || 'development';

// ============================================
// Middleware Configuration
// ============================================

const corsOptions = {
  origin: [
    'http://localhost:3000',
    'http://localhost:4000',
    'http://localhost:5000',
    'http://localhost:8080',
    'http://10.0.2.2:*',           // Android Emulator
    'http://192.168.29.133:*',     // Physical device IP (NEW)
    'http://192.168.38.40:*',      // Physical device IP (OLD)
    process.env.FRONTEND_URL,
  ].filter(Boolean),
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
};

app.use(cors(corsOptions));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] ${req.method} ${req.path}`);
  next();
});

app.get('/', (req, res) => {
  res.status(200).json({ success: true, message: 'DropSpot API Server is running' });
});

app.get('/health', (req, res) => {
  res.status(200).json({ success: true, message: 'API is healthy' });
});

app.use('/api', verifyToken);

// API Routes
app.use('/api/posts', postsRouter);
app.use('/api/requests', requestsRouter);
app.use('/api/saved', savedPostsRouter);
app.use('/api/events', eventsRouter);
app.use('/api/notifications', notificationsRouter);
app.use('/api/users', usersRouter);
app.use('/api/payments', paymentsRouter);
app.use('/api/dispatch', dispatchRouter);

app.use(notFoundHandler);
app.use(errorHandler);

const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`DropSpot API Backend Server running on http://0.0.0.0:${PORT}`);
  console.log(`Access from device: http://192.168.29.133:${PORT}`);
});

export default app;
