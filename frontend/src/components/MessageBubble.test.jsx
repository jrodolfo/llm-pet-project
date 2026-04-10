import { render, screen } from '@testing-library/react';
import MessageBubble from './MessageBubble';

describe('MessageBubble', () => {
  it('renders assistant provenance when tool metadata is present', () => {
    render(
      <MessageBubble
        role="assistant"
        content="Done."
        tool={{
          used: true,
          name: 'aws_region_audit',
          status: 'success',
          summary: 'AWS audit completed.'
        }}
      />
    );

    expect(screen.getByText('Done.')).toBeInTheDocument();
    expect(screen.getByText(/used tool: aws_region_audit/i)).toBeInTheDocument();
    expect(screen.getByText(/status: success/i)).toBeInTheDocument();
    expect(screen.getByText(/AWS audit completed./i)).toBeInTheDocument();
  });

  it('does not render provenance for assistant messages without tool metadata', () => {
    render(<MessageBubble role="assistant" content="No tool used." tool={null} />);

    expect(screen.getByText('No tool used.')).toBeInTheDocument();
    expect(screen.queryByText(/used tool:/i)).not.toBeInTheDocument();
  });

  it('does not render provenance for user messages', () => {
    render(
      <MessageBubble
        role="user"
        content="Hello"
        tool={{
          used: true,
          name: 'aws_region_audit',
          status: 'success',
          summary: 'Should be hidden.'
        }}
      />
    );

    expect(screen.getByText('Hello')).toBeInTheDocument();
    expect(screen.queryByText(/used tool:/i)).not.toBeInTheDocument();
  });
});
