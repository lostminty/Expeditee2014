package org.expeditee.items;

import javax.script.ScriptEngine;

public interface JSThreadable {
	public static final class JSThread {
		private Thread thread;
		private final String code;
		private final ScriptEngine scriptEngine;
		private boolean run = false;
		
		public JSThread(ScriptEngine scriptEngine, String code) {
			this.code = code;
			this.thread = null;
			this.scriptEngine = scriptEngine;
		}
		
		/**
		 * Stops the thread,
		 * but leaves it's state so it will run next time
		 */
		public void kill() {
			if(thread != null) {
				thread.interrupt();
				try {
	                thread.join();
                } catch (InterruptedException e) {
	                e.printStackTrace();
                }
				thread = null;
			}
		}
		
		/**
		 * Starts the thread if it isn't running and should be running
		 */
		public void resume() {
			if(!this.run)
				return;
			if(thread != null)
				return;
			thread = new Thread(new Runnable() {
    			@Override
    			public void run() {
    				try {
    	                scriptEngine.eval("thread = " + code + "\nthread()");
    	                run = false;
    				} catch (Exception e) {
    					if(e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
    						// if the thread was interrupted exit quietly
    						return;
    					}
    					e.printStackTrace();
                    }
    			}
    		});
			thread.start();
		}
		
		public boolean shouldRun() {
			return this.run;
		}
		
		public void shouldRun(boolean run) {
			this.run = run;
		}
		
		public void stop() {
			this.kill();
			this.run = false;
		}
		
		public void start() {
			this.kill();
			this.run = true;
			this.resume();
		}
	}
	
	public JSThread addThread(String code);
}
