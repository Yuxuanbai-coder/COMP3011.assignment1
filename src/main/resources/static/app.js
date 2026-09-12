const startButton = document.querySelector('#startButton');
const stopButton = document.querySelector('#stopButton');
const statusText = document.querySelector('#status');
const formatText = document.querySelector('#format');
const sizeText = document.querySelector('#size');
const audioPreview = document.querySelector('#audioPreview');
const transcriptionText = document.querySelector('#transcription');

let mediaRecorder;
let audioChunks = [];
let audioBlob;
let previewUrl;

function setStatus(message, isError = false) {
    statusText.textContent = message;
    statusText.classList.toggle('error', isError);
}

function chooseMimeType() {
    const types = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
    return types.find((type) => MediaRecorder.isTypeSupported(type)) || '';
}

async function uploadAudio(blob) {
    const formData = new FormData();
    const filename = blob.type.includes('mp4') ? 'recording.mp4' : 'recording.webm';
    formData.append('file', blob, filename);

    setStatus('Recording stopped. Transcribing...');
    transcriptionText.textContent = 'Waiting for transcription...';

    const response = await fetch('/api/v1/transcriptions', {
        method: 'POST',
        body: formData
    });

    let responseBody;
    try {
        responseBody = await response.json();
    } catch (error) {
        throw new Error(`The server returned an invalid response (${response.status}).`);
    }

    if (!response.ok) {
        throw new Error(responseBody.message || `Transcription failed (${response.status}).`);
    }

    if (typeof responseBody.text !== 'string') {
        throw new Error('The server response did not contain transcription text.');
    }

    transcriptionText.textContent = responseBody.text || '(No speech detected.)';
    setStatus('Transcription complete. Ready to record again.');
}

startButton.addEventListener('click', async () => {
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        setStatus('This browser does not support microphone recording.', true);
        return;
    }

    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        audioChunks = [];
        const mimeType = chooseMimeType();
        mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);

        mediaRecorder.addEventListener('dataavailable', (event) => {
            if (event.data.size > 0) audioChunks.push(event.data);
        });

        mediaRecorder.addEventListener('stop', () => {
            audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || 'audio/webm' });
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            previewUrl = URL.createObjectURL(audioBlob);
            audioPreview.src = previewUrl;
            audioPreview.hidden = false;
            formatText.textContent = audioBlob.type || 'audio/webm';
            sizeText.textContent = `${(audioBlob.size / 1024).toFixed(1)} KB`;
            stream.getTracks().forEach((track) => track.stop());

            uploadAudio(audioBlob)
                .catch((error) => {
                    transcriptionText.textContent = 'Transcription failed.';
                    setStatus(error.message, true);
                })
                .finally(() => {
                    startButton.disabled = false;
                    stopButton.disabled = true;
                });
        });

        mediaRecorder.start();
        startButton.disabled = true;
        stopButton.disabled = false;
        transcriptionText.textContent = 'No transcription yet.';
        formatText.textContent = mediaRecorder.mimeType || 'browser default';
        sizeText.textContent = '-';
        setStatus('Recording...');
    } catch (error) {
        setStatus(`Microphone access failed: ${error.message}`, true);
    }
});

stopButton.addEventListener('click', () => {
    if (mediaRecorder?.state === 'recording') {
        mediaRecorder.stop();
        startButton.disabled = true;
        stopButton.disabled = true;
    }
});
