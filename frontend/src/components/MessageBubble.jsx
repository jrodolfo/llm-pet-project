function MessageBubble({ role, content, tool }) {
  const isUser = role === 'user';
  const showTool = !isUser && tool?.used;

  return (
    <div className={`message-row ${isUser ? 'user' : 'assistant'}`}>
      <div className="message-bubble">
        <p>{content}</p>
        {showTool ? (
          <div className="tool-provenance">
            <span>used tool: {tool.name}</span>
            {tool.status ? <span>status: {tool.status}</span> : null}
            {tool.summary ? <span>{tool.summary}</span> : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default MessageBubble;
