function MessageBubble({ role, content }) {
  const isUser = role === 'user';

  return (
    <div className={`message-row ${isUser ? 'user' : 'assistant'}`}>
      <div className="message-bubble">
        <p>{content}</p>
      </div>
    </div>
  );
}

export default MessageBubble;
