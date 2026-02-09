import { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Switch,
  TextInput,
  Platform,
  PermissionsAndroid,
  Alert,
  Image,
} from 'react-native';
import * as FrameCapture from 'react-native-frame-capture';

export default function App() {
  // Permission state
  const [hasMediaProjection, setHasMediaProjection] = useState(false);
  const [hasNotification, setHasNotification] = useState(false);

  // Capture state
  const [isCapturing, setIsCapturing] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [frameCount, setFrameCount] = useState(0);
  const [sessionId, setSessionId] = useState<string>('');

  // Configuration
  const [interval, setInterval] = useState('1000');
  const [quality, setQuality] = useState('80');
  const [format, setFormat] = useState<'jpeg' | 'png'>('jpeg');
  const [saveFrames, setSaveFrames] = useState(false);
  const [storageLocation, setStorageLocation] = useState<'private' | 'public'>(
    'private'
  );
  const [excludeStatusBar, setExcludeStatusBar] = useState(false);

  // Capture mode configuration
  const [captureMode, setCaptureMode] = useState<
    'interval' | 'change-detection'
  >('interval');
  const [changeThreshold, setChangeThreshold] = useState('10');
  const [changeMinInterval, setChangeMinInterval] = useState('500');
  const [changeMaxInterval, setChangeMaxInterval] = useState('0');

  // Overlay configuration
  const [enableTextOverlay, setEnableTextOverlay] = useState(false);
  const [textContent, setTextContent] = useState('Frame {frameNumber}');

  // Recent frames
  const [recentFrames, setRecentFrames] = useState<
    FrameCapture.FrameCapturedEvent[]
  >([]);

  // Check permissions on mount
  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    const mediaStatus = await FrameCapture.checkPermission();
    setHasMediaProjection(
      mediaStatus === FrameCapture.PermissionStatus.GRANTED
    );

    const notifStatus = await FrameCapture.checkNotificationPermission();
    setHasNotification(notifStatus === FrameCapture.PermissionStatus.GRANTED);
  };

  // Request permissions
  const requestMediaProjection = async () => {
    try {
      const status = await FrameCapture.requestPermission();
      setHasMediaProjection(status === FrameCapture.PermissionStatus.GRANTED);
      if (status === FrameCapture.PermissionStatus.GRANTED) {
        Alert.alert('Success', 'Screen capture permission granted');
      }
    } catch (error: any) {
      Alert.alert('Error', error.message);
    }
  };

  const requestNotification = async () => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
      );
      setHasNotification(result === PermissionsAndroid.RESULTS.GRANTED);
    } else {
      setHasNotification(true);
    }
  };

  // Event handlers
  useEffect(() => {
    const subscriptions = [
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.FRAME_CAPTURED,
        (event: FrameCapture.FrameCapturedEvent) => {
          setFrameCount((prev) => prev + 1);
          setRecentFrames((prev) => [event, ...prev].slice(0, 5));
        }
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.CAPTURE_START,
        (event: FrameCapture.CaptureStartEvent) => {
          setSessionId(event.sessionId);
          setIsCapturing(true);
          setIsPaused(false);
          setFrameCount(0);
          setRecentFrames([]);
        }
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.CAPTURE_STOP,
        (event: FrameCapture.CaptureStopEvent) => {
          setIsCapturing(false);
          setIsPaused(false);
          Alert.alert(
            'Capture Stopped',
            `Captured ${event.totalFrames} frames in ${(event.duration / 1000).toFixed(1)}s`
          );
        }
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.CAPTURE_PAUSE,
        () => setIsPaused(true)
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.CAPTURE_RESUME,
        () => setIsPaused(false)
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.CAPTURE_ERROR,
        (event: FrameCapture.CaptureErrorEvent) =>
          Alert.alert('Capture Error', event.message)
      ),
      FrameCapture.addListener(
        FrameCapture.CaptureEventType.STORAGE_WARNING,
        (event: FrameCapture.StorageWarningEvent) =>
          Alert.alert(
            'Low Storage',
            `Only ${(event.availableSpace / 1024 / 1024).toFixed(0)}MB available`
          )
      ),
    ];

    return () => subscriptions.forEach((sub) => sub.remove());
  }, []);

  // Capture controls
  const handleStartCapture = async () => {
    if (!hasMediaProjection) {
      Alert.alert(
        'Permission Required',
        'Please grant screen capture permission first'
      );
      return;
    }

    try {
      const overlays: FrameCapture.OverlayConfig[] = [];
      if (enableTextOverlay) {
        overlays.push({
          type: 'text',
          content: textContent,
          position: 'bottom-right',
          style: {
            fontSize: 14,
            color: '#FFFFFF',
            backgroundColor: '#00000099',
            padding: 8,
            fontWeight: 'bold',
          },
        });
      }

      await FrameCapture.startCapture({
        capture:
          captureMode === 'interval'
            ? {
                mode: 'interval',
                interval: parseInt(interval, 10),
              }
            : {
                mode: 'change-detection',
                changeDetection: {
                  threshold: parseFloat(changeThreshold),
                  minInterval: parseInt(changeMinInterval, 10),
                  maxInterval: parseInt(changeMaxInterval, 10),
                },
              },
        image: {
          quality: parseInt(quality, 10),
          format,
          excludeStatusBar,
        },
        storage: {
          saveFrames,
          location: storageLocation,
        },
        overlays: overlays.length > 0 ? overlays : undefined,
        notification: {
          title: 'Screen Capture Active',
          description:
            captureMode === 'interval'
              ? 'Captured {frameCount} frames'
              : 'Change detection mode - {frameCount} frames',
          showFrameCount: true,
          updateInterval: 10,
          showPauseAction: true,
          showStopAction: true,
        },
      });
    } catch (error: any) {
      Alert.alert('Error', error.message);
    }
  };

  const handleStopCapture = async () => {
    try {
      await FrameCapture.stopCapture();
    } catch (error: any) {
      Alert.alert('Error', error.message);
    }
  };

  const handlePauseCapture = async () => {
    try {
      await FrameCapture.pauseCapture();
    } catch (error: any) {
      Alert.alert('Error', error.message);
    }
  };

  const handleResumeCapture = async () => {
    try {
      await FrameCapture.resumeCapture();
    } catch (error: any) {
      Alert.alert('Error', error.message);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Frame Capture Example</Text>
        <Text style={styles.subtitle}>React Native Frame Capture Demo</Text>
      </View>

      {/* Permissions Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Permissions</Text>

        <View style={styles.permissionRow}>
          <View style={styles.permissionInfo}>
            <Text style={styles.permissionLabel}>Screen Capture</Text>
            <Text style={styles.permissionStatus}>
              {hasMediaProjection ? '✅ Granted' : '❌ Not Granted'}
            </Text>
          </View>
          <TouchableOpacity
            style={[
              styles.button,
              styles.smallButton,
              hasMediaProjection && styles.buttonDisabled,
            ]}
            onPress={requestMediaProjection}
            disabled={hasMediaProjection}
          >
            <Text style={styles.buttonText}>Request</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.permissionRow}>
          <View style={styles.permissionInfo}>
            <Text style={styles.permissionLabel}>
              Notifications (Android 13+)
            </Text>
            <Text style={styles.permissionStatus}>
              {hasNotification ? '✅ Granted' : '❌ Not Granted'}
            </Text>
          </View>
          <TouchableOpacity
            style={[
              styles.button,
              styles.smallButton,
              hasNotification && styles.buttonDisabled,
            ]}
            onPress={requestNotification}
            disabled={hasNotification}
          >
            <Text style={styles.buttonText}>Request</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Status Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Status</Text>
        <Text style={styles.statusText}>
          State: {isCapturing ? (isPaused ? 'Paused' : 'Capturing') : 'Idle'}
        </Text>
        <Text style={styles.statusText}>Frames Captured: {frameCount}</Text>
        {sessionId && (
          <Text style={styles.statusText}>
            Session: {sessionId.substring(0, 8)}...
          </Text>
        )}
      </View>

      {/* Configuration Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Configuration</Text>

        {/* Capture Mode Selection */}
        <View style={styles.configRow}>
          <Text style={styles.label}>Capture Mode:</Text>
          <View style={styles.formatButtons}>
            <TouchableOpacity
              style={[
                styles.formatButton,
                captureMode === 'interval' && styles.formatButtonActive,
              ]}
              onPress={() => setCaptureMode('interval')}
              disabled={isCapturing}
            >
              <Text
                style={[
                  styles.formatButtonText,
                  captureMode === 'interval' && styles.formatButtonTextActive,
                ]}
              >
                Interval
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.formatButton,
                captureMode === 'change-detection' && styles.formatButtonActive,
              ]}
              onPress={() => setCaptureMode('change-detection')}
              disabled={isCapturing}
            >
              <Text
                style={[
                  styles.formatButtonText,
                  captureMode === 'change-detection' &&
                    styles.formatButtonTextActive,
                ]}
              >
                Change Detection
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Interval Mode Settings */}
        {captureMode === 'interval' && (
          <View style={styles.configRow}>
            <Text style={styles.label}>Interval (ms):</Text>
            <TextInput
              style={styles.input}
              value={interval}
              onChangeText={setInterval}
              keyboardType="numeric"
              editable={!isCapturing}
            />
          </View>
        )}

        {/* Change Detection Mode Settings */}
        {captureMode === 'change-detection' && (
          <>
            <View style={styles.configRow}>
              <Text style={styles.label}>Threshold (%):</Text>
              <TextInput
                style={styles.input}
                value={changeThreshold}
                onChangeText={setChangeThreshold}
                keyboardType="numeric"
                placeholder="1-100"
                editable={!isCapturing}
              />
            </View>
            <View style={styles.configRow}>
              <Text style={styles.label}>Min Interval (ms):</Text>
              <TextInput
                style={styles.input}
                value={changeMinInterval}
                onChangeText={setChangeMinInterval}
                keyboardType="numeric"
                editable={!isCapturing}
              />
            </View>
            <View style={styles.configRow}>
              <Text style={styles.label}>Max Interval (ms):</Text>
              <TextInput
                style={styles.input}
                value={changeMaxInterval}
                onChangeText={setChangeMaxInterval}
                keyboardType="numeric"
                placeholder="0 = no max"
                editable={!isCapturing}
              />
            </View>
          </>
        )}

        <View style={styles.configRow}>
          <Text style={styles.label}>Quality (0-100):</Text>
          <TextInput
            style={styles.input}
            value={quality}
            onChangeText={setQuality}
            keyboardType="numeric"
            editable={!isCapturing}
          />
        </View>

        <View style={styles.configRow}>
          <Text style={styles.label}>Format:</Text>
          <View style={styles.formatButtons}>
            <TouchableOpacity
              style={[
                styles.formatButton,
                format === 'jpeg' && styles.formatButtonActive,
              ]}
              onPress={() => setFormat('jpeg')}
              disabled={isCapturing}
            >
              <Text
                style={[
                  styles.formatButtonText,
                  format === 'jpeg' && styles.formatButtonTextActive,
                ]}
              >
                JPEG
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.formatButton,
                format === 'png' && styles.formatButtonActive,
              ]}
              onPress={() => setFormat('png')}
              disabled={isCapturing}
            >
              <Text
                style={[
                  styles.formatButtonText,
                  format === 'png' && styles.formatButtonTextActive,
                ]}
              >
                PNG
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.switchRow}>
          <Text style={styles.label}>Save Frames</Text>
          <Switch
            value={saveFrames}
            onValueChange={setSaveFrames}
            disabled={isCapturing}
          />
        </View>

        {saveFrames && (
          <View style={styles.configRow}>
            <Text style={styles.label}>Location:</Text>
            <View style={styles.formatButtons}>
              <TouchableOpacity
                style={[
                  styles.formatButton,
                  storageLocation === 'private' && styles.formatButtonActive,
                ]}
                onPress={() => setStorageLocation('private')}
                disabled={isCapturing}
              >
                <Text
                  style={[
                    styles.formatButtonText,
                    storageLocation === 'private' &&
                      styles.formatButtonTextActive,
                  ]}
                >
                  Private
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.formatButton,
                  storageLocation === 'public' && styles.formatButtonActive,
                ]}
                onPress={() => setStorageLocation('public')}
                disabled={isCapturing}
              >
                <Text
                  style={[
                    styles.formatButtonText,
                    storageLocation === 'public' &&
                      styles.formatButtonTextActive,
                  ]}
                >
                  Public
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        <View style={styles.switchRow}>
          <Text style={styles.label}>Exclude Status Bar</Text>
          <Switch
            value={excludeStatusBar}
            onValueChange={setExcludeStatusBar}
            disabled={isCapturing}
          />
        </View>
      </View>

      {/* Overlay Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Text Overlay</Text>

        <View style={styles.switchRow}>
          <Text style={styles.label}>Enable Text Overlay</Text>
          <Switch
            value={enableTextOverlay}
            onValueChange={setEnableTextOverlay}
            disabled={isCapturing}
          />
        </View>

        {enableTextOverlay && (
          <View style={styles.configRow}>
            <Text style={styles.label}>Text:</Text>
            <TextInput
              style={styles.input}
              value={textContent}
              onChangeText={setTextContent}
              placeholder="{frameNumber}, {timestamp}"
              editable={!isCapturing}
            />
          </View>
        )}
      </View>

      {/* Controls Section */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Controls</Text>

        <TouchableOpacity
          style={[
            styles.button,
            styles.primaryButton,
            isCapturing && styles.buttonDisabled,
          ]}
          onPress={handleStartCapture}
          disabled={isCapturing || !hasMediaProjection}
        >
          <Text style={styles.buttonText}>Start Capture</Text>
        </TouchableOpacity>

        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[
              styles.button,
              styles.secondaryButton,
              styles.halfButton,
              (!isCapturing || isPaused) && styles.buttonDisabled,
            ]}
            onPress={handlePauseCapture}
            disabled={!isCapturing || isPaused}
          >
            <Text style={styles.buttonText}>Pause</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.button,
              styles.secondaryButton,
              styles.halfButton,
              !isPaused && styles.buttonDisabled,
            ]}
            onPress={handleResumeCapture}
            disabled={!isPaused}
          >
            <Text style={styles.buttonText}>Resume</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[
            styles.button,
            styles.dangerButton,
            !isCapturing && styles.buttonDisabled,
          ]}
          onPress={handleStopCapture}
          disabled={!isCapturing}
        >
          <Text style={styles.buttonText}>Stop Capture</Text>
        </TouchableOpacity>
      </View>

      {/* Recent Frames Section */}
      {recentFrames.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>
            Recent Frames ({recentFrames.length})
          </Text>
          {recentFrames.map((frame, index) => (
            <View key={index} style={styles.frameItem}>
              <Image
                source={{ uri: `file://${frame.filePath}` }}
                style={styles.frameThumbnail}
                resizeMode="cover"
              />
              <View style={styles.frameInfo}>
                <Text style={styles.frameText}>Frame #{frame.frameNumber}</Text>
                <Text style={styles.frameSubtext}>
                  {(frame.fileSize / 1024).toFixed(1)} KB
                </Text>
                <Text style={styles.frameSubtext} numberOfLines={1}>
                  {frame.filePath.split('/').pop()}
                </Text>
              </View>
            </View>
          ))}
        </View>
      )}

      <View style={styles.footer}>
        <Text style={styles.footerText}>React Native Frame Capture v1.0.0</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#007AFF',
    padding: 20,
    paddingTop: 40,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#fff',
    opacity: 0.9,
  },
  section: {
    backgroundColor: '#fff',
    margin: 12,
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
    color: '#333',
  },
  permissionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  permissionInfo: {
    flex: 1,
  },
  permissionLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  permissionStatus: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  statusText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  configRow: {
    marginBottom: 12,
  },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 6,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 10,
    fontSize: 14,
    backgroundColor: '#f9f9f9',
  },
  formatButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  formatButton: {
    flex: 1,
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    backgroundColor: '#f9f9f9',
    alignItems: 'center',
  },
  formatButtonActive: {
    backgroundColor: '#007AFF',
    borderColor: '#007AFF',
  },
  formatButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
  },
  formatButtonTextActive: {
    color: '#fff',
  },
  button: {
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 8,
  },
  primaryButton: {
    backgroundColor: '#007AFF',
  },
  secondaryButton: {
    backgroundColor: '#5856D6',
  },
  dangerButton: {
    backgroundColor: '#FF3B30',
  },
  smallButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    marginBottom: 0,
    backgroundColor: '#007AFF',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 8,
  },
  halfButton: {
    flex: 1,
  },
  frameItem: {
    flexDirection: 'row',
    padding: 8,
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    marginBottom: 8,
  },
  frameThumbnail: {
    width: 60,
    height: 60,
    borderRadius: 4,
    backgroundColor: '#ddd',
  },
  frameInfo: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'center',
  },
  frameText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  frameSubtext: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  footer: {
    padding: 20,
    alignItems: 'center',
  },
  footerText: {
    fontSize: 12,
    color: '#999',
  },
});
