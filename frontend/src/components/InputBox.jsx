import { useState } from 'react';

const MODEL_OPTIONS = ['codellama:70b', 'llama3:8b', 'mistral:7b'];

function InputBox({ disabled, onSend }) {
  const [message, setMessage] = useState('');
  const [model, setModel] = useState(MODEL_OPTIONS[0]);
  const [streaming, setStreaming] = useState(true);

  const submit = (event) => {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) {
      return;
    }

    onSend({ message: trimmed, model, streaming });
    setMessage('');
  };

  return (
    <form className="input-box" onSubmit={submit}>
      <div className="controls-row">
        <select value={model} onChange={(event) => setModel(event.target.value)} disabled={disabled}>
          {MODEL_OPTIONS.map((option) => (
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
            disabled={disabled}
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
          disabled={disabled}
        />
        <button type="submit" disabled={disabled || !message.trim()}>
          Send
        </button>
      </div>
    </form>
  );
}

export default InputBox;
