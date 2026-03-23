import { v4 as uuidv4 } from 'uuid';

/**
 * Generate unique ID
 */
export const generateId = () => uuidv4();

/**
 * Format timestamp
 */
export const getCurrentTimestamp = () => new Date().toISOString();

/**
 * Validate email
 */
export const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validate coordinates
 */
export const isValidCoordinates = (latitude, longitude) => {
  return (
    typeof latitude === 'number' &&
    typeof longitude === 'number' &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180
  );
};

/**
 * Calculate distance between two coordinates (Haversine formula)
 */
export const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // Earth's radius in kilometers
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const distance = R * c;
  return Math.round(distance * 10) / 10; // Round to 1 decimal
};

/**
 * Create success response
 */
export const successResponse = (data = null, message = 'Success') => {
  return {
    success: true,
    message,
    data,
  };
};

/**
 * Create error response
 */
export const errorResponse = (message = 'Error', error = 'SERVER_ERROR', code = 500) => {
  return {
    success: false,
    message,
    error,
    code,
  };
};

/**
 * Validate post data
 */
export const validatePostData = (data) => {
  const errors = {};

  if (!data.title || typeof data.title !== 'string' || data.title.trim().length === 0) {
    errors.title = 'Title is required and must be a non-empty string';
  }

  if (!data.description || typeof data.description !== 'string' || data.description.trim().length === 0) {
    errors.description = 'Description is required and must be a non-empty string';
  }

  if (!data.category || typeof data.category !== 'string') {
    errors.category = 'Category is required';
  }

  if (data.latitude !== undefined && data.longitude !== undefined) {
    if (!isValidCoordinates(data.latitude, data.longitude)) {
      errors.coordinates = 'Invalid latitude or longitude';
    }
  }

  if (data.images && !Array.isArray(data.images)) {
    errors.images = 'Images must be an array';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
};

/**
 * Validate request data
 */
export const validateRequestData = (data) => {
  const errors = {};

  if (!data.postId || typeof data.postId !== 'string') {
    errors.postId = 'Post ID is required';
  }

  if (!data.message || typeof data.message !== 'string') {
    errors.message = 'Message is required';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
};

/**
 * Sanitize user data (remove sensitive fields)
 */
export const sanitizeUserData = (user) => {
  const { uid, email, displayName, photoURL, ...rest } = user;
  return {
    uid,
    email,
    displayName,
    photoURL,
  };
};
