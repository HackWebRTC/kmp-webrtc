// credit: https://blog.mozilla.org/webrtc/warm-up-with-replacetrack/
// let black = ({width = 640, height = 480} = {}) => {
//    let canvas = Object.assign(document.createElement("canvas"), {width, height});
//    canvas.getContext('2d').fillRect(0, 0, width, height);
//    let stream = canvas.captureStream();
//    return Object.assign(stream.getVideoTracks()[0], {enabled: false});
//  }
function createBlackStream(options = { width: 640, height: 480 }) {
    const { width = 640, height = 480 } = options;

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;

    canvas.getContext('2d').fillRect(0, 0, width, height);

    const track = canvas.captureStream().getVideoTracks()[0];
    track.enabled = false;

    return track;
}
