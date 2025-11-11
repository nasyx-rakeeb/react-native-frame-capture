import { validateOptions, mergeOptions } from '../validation';
import { CaptureError } from '../errors';

describe('validation', () => {
  describe('validateOptions', () => {
    describe('capture.interval', () => {
      it('should accept valid interval', () => {
        expect(() => {
          validateOptions({ capture: { interval: 1000 } });
        }).not.toThrow();
      });

      it('should reject interval below minimum', () => {
        expect(() => {
          validateOptions({ capture: { interval: 50 } });
        }).toThrow(CaptureError);
      });

      it('should reject interval above maximum', () => {
        expect(() => {
          validateOptions({ capture: { interval: 70000 } });
        }).toThrow(CaptureError);
      });

      it('should reject non-number interval', () => {
        expect(() => {
          validateOptions({ capture: { interval: '1000' as any } });
        }).toThrow(CaptureError);
      });
    });

    describe('image.quality', () => {
      it('should accept valid quality', () => {
        expect(() => {
          validateOptions({ image: { quality: 80, format: 'jpeg' } });
        }).not.toThrow();
      });

      it('should reject quality below 0', () => {
        expect(() => {
          validateOptions({ image: { quality: -1, format: 'jpeg' } });
        }).toThrow(CaptureError);
      });

      it('should reject quality above 100', () => {
        expect(() => {
          validateOptions({ image: { quality: 101, format: 'jpeg' } });
        }).toThrow(CaptureError);
      });
    });

    describe('image.format', () => {
      it('should accept jpeg format', () => {
        expect(() => {
          validateOptions({ image: { quality: 80, format: 'jpeg' } });
        }).not.toThrow();
      });

      it('should accept png format', () => {
        expect(() => {
          validateOptions({ image: { quality: 80, format: 'png' } });
        }).not.toThrow();
      });

      it('should reject invalid format', () => {
        expect(() => {
          validateOptions({ image: { quality: 80, format: 'webp' as any } });
        }).toThrow(CaptureError);
      });
    });

    describe('image.scaleResolution', () => {
      it('should accept valid scale', () => {
        expect(() => {
          validateOptions({
            image: { quality: 80, format: 'jpeg', scaleResolution: 0.5 },
          });
        }).not.toThrow();
      });

      it('should reject scale below 0.1', () => {
        expect(() => {
          validateOptions({
            image: { quality: 80, format: 'jpeg', scaleResolution: 0.05 },
          });
        }).toThrow(CaptureError);
      });

      it('should reject scale above 1.0', () => {
        expect(() => {
          validateOptions({
            image: { quality: 80, format: 'jpeg', scaleResolution: 1.5 },
          });
        }).toThrow(CaptureError);
      });
    });

    describe('storage.location', () => {
      it('should accept private location', () => {
        expect(() => {
          validateOptions({ storage: { location: 'private' } });
        }).not.toThrow();
      });

      it('should accept public location', () => {
        expect(() => {
          validateOptions({ storage: { location: 'public' } });
        }).not.toThrow();
      });

      it('should reject invalid location', () => {
        expect(() => {
          validateOptions({ storage: { location: 'external' as any } });
        }).toThrow(CaptureError);
      });
    });

    describe('storage.warningThreshold', () => {
      it('should accept valid threshold', () => {
        expect(() => {
          validateOptions({ storage: { warningThreshold: 50 * 1024 * 1024 } });
        }).not.toThrow();
      });

      it('should reject negative threshold', () => {
        expect(() => {
          validateOptions({ storage: { warningThreshold: -1 } });
        }).toThrow(CaptureError);
      });
    });
  });

  describe('mergeOptions', () => {
    it('should use defaults when no options provided', () => {
      const result = mergeOptions();
      expect(result.capture.interval).toBe(1000);
      expect(result.image.quality).toBe(80);
      expect(result.image.format).toBe('jpeg');
    });

    it('should merge user options with defaults', () => {
      const result = mergeOptions({
        capture: { interval: 2000 },
      });
      expect(result.capture.interval).toBe(2000);
      expect(result.image.quality).toBe(80);
    });

    it('should override defaults with user options', () => {
      const result = mergeOptions({
        capture: { interval: 500 },
        image: { quality: 90, format: 'png' },
      });
      expect(result.capture.interval).toBe(500);
      expect(result.image.quality).toBe(90);
      expect(result.image.format).toBe('png');
    });
  });
});
