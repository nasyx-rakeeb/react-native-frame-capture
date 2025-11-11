import {
  DEFAULT_OPTIONS,
  DEFAULT_ADVANCED_CONFIG,
  MIN_INTERVAL,
  MAX_INTERVAL,
  MIN_QUALITY,
  MAX_QUALITY,
} from '../constants';

describe('constants', () => {
  describe('DEFAULT_OPTIONS', () => {
    it('should have valid default capture interval', () => {
      expect(DEFAULT_OPTIONS.capture.interval).toBe(1000);
      expect(DEFAULT_OPTIONS.capture.interval).toBeGreaterThanOrEqual(
        MIN_INTERVAL
      );
      expect(DEFAULT_OPTIONS.capture.interval).toBeLessThanOrEqual(
        MAX_INTERVAL
      );
    });

    it('should have valid default image quality', () => {
      expect(DEFAULT_OPTIONS.image.quality).toBe(80);
      expect(DEFAULT_OPTIONS.image.quality).toBeGreaterThanOrEqual(MIN_QUALITY);
      expect(DEFAULT_OPTIONS.image.quality).toBeLessThanOrEqual(MAX_QUALITY);
    });

    it('should have valid default image format', () => {
      expect(DEFAULT_OPTIONS.image.format).toBe('jpeg');
      expect(['jpeg', 'png']).toContain(DEFAULT_OPTIONS.image.format);
    });
  });

  describe('DEFAULT_ADVANCED_CONFIG', () => {
    it('should have storage config', () => {
      expect(DEFAULT_ADVANCED_CONFIG.storage).toBeDefined();
      expect(DEFAULT_ADVANCED_CONFIG.storage.warningThreshold).toBe(
        100 * 1024 * 1024
      );
    });

    it('should have file naming config', () => {
      expect(DEFAULT_ADVANCED_CONFIG.fileNaming).toBeDefined();
      expect(DEFAULT_ADVANCED_CONFIG.fileNaming.prefix).toBe('capture_');
      expect(DEFAULT_ADVANCED_CONFIG.fileNaming.framePadding).toBe(5);
    });

    it('should have performance config', () => {
      expect(DEFAULT_ADVANCED_CONFIG.performance).toBeDefined();
      expect(DEFAULT_ADVANCED_CONFIG.performance.imageReaderBuffers).toBe(2);
    });
  });

  describe('interval constraints', () => {
    it('should have valid min interval', () => {
      expect(MIN_INTERVAL).toBe(100);
      expect(MIN_INTERVAL).toBeGreaterThan(0);
    });

    it('should have valid max interval', () => {
      expect(MAX_INTERVAL).toBe(60000);
      expect(MAX_INTERVAL).toBeGreaterThan(MIN_INTERVAL);
    });
  });

  describe('quality constraints', () => {
    it('should have valid min quality', () => {
      expect(MIN_QUALITY).toBe(0);
    });

    it('should have valid max quality', () => {
      expect(MAX_QUALITY).toBe(100);
      expect(MAX_QUALITY).toBeGreaterThan(MIN_QUALITY);
    });
  });
});
