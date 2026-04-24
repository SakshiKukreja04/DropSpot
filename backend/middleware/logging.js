export const requestLogger = (req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`[${timestamp}] ${req.method} ${req.path}`);

  const startTime = Date.now();
  const originalSend = res.send;

  res.send = function(data) {
    const duration = Date.now() - startTime;
    console.log(`  Response: ${res.statusCode} - ${duration}ms`);
    return originalSend.call(this, data);
  };

  next();
};
