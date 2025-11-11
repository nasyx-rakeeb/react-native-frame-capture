import { normalizeOptions } from '../normalize';
import type { CaptureOptions } from '../types';

describe('normalize', () => {
  describe('normalizeOptions', () => {
    it('should normalize basic options', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: { quality: 80, format: 'jpeg' },
      };

      const result = normalizeOptions(options);

      expect(result.interval).toBe(1000);
      expect(result.quality).toBe(80);
      expect(result.format).toBe('jpeg');
    });

    it('should normalize storage options', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: { quality: 80, format: 'jpeg' },
        storage: {
          saveFrames: true,
          location: 'public',
          outputDirectory: '/storage/emulated/0/DCIM',
        },
      };

      const result = normalizeOptions(options);

      expect(result.saveFrames).toBe(true);
      expect(result.storageLocation).toBe('public');
      expect(result.outputDirectory).toBe('/storage/emulated/0/DCIM');
    });

    it('should normalize image options', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: {
          quality: 80,
          format: 'png',
          scaleResolution: 0.5,
          excludeStatusBar: true,
          region: { x: 0.1, y: 0.1, width: 0.8, height: 0.8 },
        },
      };

      const result = normalizeOptions(options);

      expect(result.format).toBe('png');
      expect(result.scaleResolution).toBe(0.5);
      expect(result.excludeStatusBar).toBe(true);
      expect(result.captureRegion).toEqual({
        x: 0.1,
        y: 0.1,
        width: 0.8,
        height: 0.8,
      });
    });

    it('should normalize advanced config', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: { quality: 80, format: 'jpeg' },
        storage: {
          warningThreshold: 50 * 1024 * 1024,
          fileNaming: {
            prefix: 'frame_',
            dateFormat: 'yyyyMMdd',
            framePadding: 6,
          },
        },
        performance: {
          overlayCacheSize: 20 * 1024 * 1024,
          imageReaderBuffers: 3,
        },
      };

      const result = normalizeOptions(options);

      expect(result.advanced?.storage?.warningThreshold).toBe(50 * 1024 * 1024);
      expect(result.advanced?.fileNaming?.prefix).toBe('frame_');
      expect(result.advanced?.performance?.overlayCacheSize).toBe(
        20 * 1024 * 1024
      );
    });

    it('should handle overlays', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: { quality: 80, format: 'jpeg' },
        overlays: [
          {
            type: 'text',
            content: 'Frame {frameNumber}',
            position: 'bottom-right',
          },
        ],
      };

      const result = normalizeOptions(options);

      expect(result.overlays).toHaveLength(1);
      expect(result.overlays?.[0].type).toBe('text');
    });

    it('should handle notification options', () => {
      const options: CaptureOptions = {
        capture: { interval: 1000 },
        image: { quality: 80, format: 'jpeg' },
        notification: {
          title: 'Recording',
          description: 'Captured {frameCount} frames',
          showFrameCount: true,
        },
      };

      const result = normalizeOptions(options);

      expect(result.notification).toBeDefined();
      expect(result.notification?.title).toBe('Recording');
    });
  });
});
