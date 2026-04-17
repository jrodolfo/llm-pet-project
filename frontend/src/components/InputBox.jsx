import { useState } from 'react';

function InputBox({
  disabled,
  loadingMessage = '',
  statusMessage = '',
  providers = [],
  selectedProvider = '',
  onProviderChange,
  models = [],
  selectedModel = '',
  onModelChange,
  onSend
}) {
  const [message, setMessage] = useState('');
  const [streaming, setStreaming] = useState(true);
  const sendDisabled = disabled || !selectedModel;

  const submit = (event) => {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed || !selectedModel) {
      return;
    }

    onSend({ message: trimmed, provider: selectedProvider, model: selectedModel, streaming });
    setMessage('');
  };

  return (
    <form className="input-box" onSubmit={submit}>
      <div className="controls-row">
        <select
          aria-label="Chat provider"
          value={selectedProvider}
          onChange={(event) => onProviderChange(event.target.value)}
          disabled={disabled}
        >
          {providers.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>

        <select
          aria-label="Model"
          value={selectedModel}
          onChange={(event) => onModelChange(event.target.value)}
          disabled={sendDisabled}
        >
          {models.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>

        <label className="checkbox-wrap">
          <input
            type="checkbox"
            checked={streaming}
            onChange={(event) => setStreaming(event.target.checked)}
            disabled={sendDisabled}
          />
          Streaming
        </label>
      </div>

      <div className="composer-row">
        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="Type your prompt..."
          rows={3}
          disabled={sendDisabled}
        />
        <button type="submit" disabled={sendDisabled || !message.trim()}>
          {disabled ? 'Working...' : 'Send'}
        </button>
      </div>
      {disabled && loadingMessage ? <p className="input-status">{loadingMessage}</p> : null}
      {!disabled && statusMessage ? <p className="input-status">{statusMessage}</p> : null}
    </form>
  );
}

export default InputBox;
