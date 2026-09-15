import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, ArrowLeft } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallbackTitle?: string;
  onReset?: () => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
  }

  public handleReset = () => {
    this.setState({ hasError: false, error: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="flex-1 flex items-center justify-center p-6 h-full min-h-[300px]">
          <div className="bg-white border-2 border-red-200 rounded-3xl p-6 max-w-md w-full shadow-2xl text-center flex flex-col items-center">
            <div className="w-14 h-14 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mb-4">
              <AlertTriangle className="w-8 h-8" />
            </div>
            <h2 className="text-lg font-black text-gray-900 mb-1">
              {this.props.fallbackTitle || 'Something went wrong'}
            </h2>
            <p className="text-xs text-gray-500 mb-4 leading-relaxed">
              An unexpected error occurred while rendering this section. Your settings and study sessions are safe.
            </p>
            {this.state.error && (
              <div className="bg-red-50 text-red-800 text-[11px] font-mono p-3 rounded-xl mb-6 text-left w-full max-h-32 overflow-y-auto break-all border border-red-100">
                {this.state.error.message || String(this.state.error)}
              </div>
            )}
            <div className="flex gap-3 w-full">
              <button
                onClick={this.handleReset}
                className="flex-1 py-3 px-4 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl text-xs flex items-center justify-center gap-1.5 transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                Return to Dashboard
              </button>
              <button
                onClick={() => window.location.reload()}
                className="flex-1 py-3 px-4 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl text-xs flex items-center justify-center gap-1.5 transition-colors shadow-md"
              >
                <RefreshCw className="w-4 h-4" />
                Reload App
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
