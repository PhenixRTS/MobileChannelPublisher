/**
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All Rights Reserved.
 */

import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
  LogBox
} from 'react-native';
import {
  RTCPeerConnection,
  RTCIceCandidate,
  RTCSessionDescription,
  RTCView,
  mediaDevices,
  MediaTrackConstraints,
  MediaStream
} from 'react-native-webrtc';

import {CustomInput, CustomButton} from './components/ui';

// eslint-disable-next-line no-global-assign
global = Object.assign(global, {
  RTCPeerConnection,
  RTCIceCandidate,
  RTCSessionDescription
});

LogBox.ignoreAllLogs();

const sdk = require('phenix-web-sdk/dist/phenix-web-sdk-react-native');

sdk.RTC.shim(); // Required

// authToken
const TOKEN = '';
// PublishToken:
const PUBLISH_TOKEN = '';
const VIDEO_INPUT = 'videoinput';

enum FacingType {
  User = 'user',
  Environment = 'environment'
}
interface VideoDevicesProps {
  deviceId: string;
  facing: string;
  kind: string;
}

const App = () => {
  const webrtcSupported = sdk.RTC.webrtcSupported;
  const [videoDevices, setVideoDevices] = useState<VideoDevicesProps[]>([]);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [authToken, setAuthToken] = useState<string>(TOKEN);
  const [publishToken, setPublishToken] = useState<string>(PUBLISH_TOKEN);
  const [channel, setChannel] = useState();
  const [isPublishing, setIsPublishing] = useState<boolean>(false);
  const [facing, setFacing] = useState<MediaTrackConstraints['facingMode']>(FacingType.Environment);

  useEffect(() => {
    mediaDevices.enumerateDevices()
      .then((devices: VideoDevicesProps[]) => {
        const videoDevs = devices.filter(val => val.kind === VIDEO_INPUT);
        setVideoDevices(videoDevs);

        const facingMode = videoDevs[0].facing === FacingType.Environment ? FacingType.Environment : FacingType.User;
        setFacing(facingMode);

        const sourceId = videoDevices.find(val => val.facing === facingMode)?.deviceId || '0';
        const video: MediaTrackConstraints = {
          facingMode,
          mandatory: {
            minWidth: 640,
            minHeight: 480,
            minFrameRate: 15
          },
          optional: [{sourceId}]
        };

        mediaDevices.getUserMedia({
          video,
          audio: true
        })
          .then((stream) => {
            if (stream && stream instanceof MediaStream) {
              setStream(stream);
            }
          })
          .catch(() => {});
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (channel && publishToken && stream) {
      const publishOptions = {
        authToken,
        publishToken,
        channel: {},
        userMediaStream: stream
      };
      channel.publishToChannel(
        publishOptions,
        () => {}
      );
    }
  }, [channel]);

  useEffect(() => {
    stream?.getVideoTracks().forEach((track)=> {
      track._switchCamera();
    });
  }, [facing]);

  const onSubmit = () => {
    if (stream && authToken && publishToken && !isPublishing) {
      setIsPublishing(true);

      const channelExpress = new sdk.express.ChannelExpress({authToken});
      setChannel(channelExpress);
    }
  };

  const onCancel = () => {
    if (channel) {
      channel.dispose();
    }

    if (isPublishing) {
      setIsPublishing(false);
    }
  };

  const toggleCamera = () => {
    const newFacing = facing === FacingType.User ? FacingType.Environment : FacingType.User;
    setFacing(newFacing);
  };

  const videoURL = stream?.toURL() || '';
  const isSwitchCamera = videoDevices.length > 1;

  return (
    <View style={styles.wrapper}>
      <SafeAreaView style={styles.flex}>
        <ScrollView style={styles.flex}>
          <View style={styles.container}>
            <Text style={styles.text}> {'WebRTC is supported: ' + (webrtcSupported ? 'YES' : 'NO')}</Text>

            <RTCView
              style={styles.video}
              streamURL={videoURL}
            />

            <View style={styles.inputWrapper}>
              <CustomInput
                label="AuthToken"
                value={authToken}
                placeholder={'Enter Auth Token'}
                onChangeText={setAuthToken}
                onClear={() => setAuthToken('')}
              />
            </View>

            <View style={styles.inputWrapper}>
              <CustomInput
                label="PublishToken"
                value={publishToken}
                placeholder={'Enter Publish Token'}
                onChangeText={setPublishToken}
                onClear={() => setPublishToken('')}
              />
            </View>

            <View style={styles.btns}>
              {isSwitchCamera && (
                <View style={styles.btn}>
                  <CustomButton
                    title={facing === FacingType.User ? 'Rear Camera' : 'Front Camera'}
                    onPress={toggleCamera}
                  />
                </View>
              )}
              <View style={styles.btn}>
                <CustomButton
                  title={isPublishing ? 'CANCEL' : 'PUBLISH'}
                  onPress={isPublishing ? onCancel : onSubmit}
                />
              </View>
            </View>

            <View style={styles.isPublishingWrapper}>
              <Text style={styles.isPublishing}>{isPublishing ? 'Publishing' : 'Not Publishing'}</Text>
            </View>

          </View>
        </ScrollView>
      </SafeAreaView>
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  flex: {flex: 1},
  text: {color: 'black'},
  wrapper: {
    flex: 1,
    backgroundColor: '#fff'
  },
  container: {
    flex: 1,
    padding: 16,
    alignItems: 'center',
    color: 'black'
  },
  video: {
    height: 360,
    width: '100%'
  },
  inputWrapper: {
    flex: 1,
    width: '100%',
    marginTop: 8
  },
  btns: {
    flex: 1,
    flexDirection: 'row',
    marginTop: 16,
    justifyContent: 'space-between'
  },
  btn: {
    minWidth: 100,
    margin: 16
  },
  isPublishingWrapper: {
    position: 'absolute',
    top: 4,
    right: 8
  },
  isPublishing: {color: 'blue'}
});