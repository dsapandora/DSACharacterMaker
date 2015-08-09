package charactermanaj.model.io;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;

/**
 * ディレクトリ監視エージェントのファクトリ.<br>
 * このファクトリ単位でディレクト利監視エージェントは保持されます.<br>
 * このファクトリ上に、すでに同一のキャラクターデータを監視するエージェントがいる場合、それらのエージェントの実体は
 * ハンドルによって共有されます.<br> 
 * @author seraphy
 */
public final class PartsImageDirectoryWatchAgentFactory {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private static PartsImageDirectoryWatchAgentFactory inst = new PartsImageDirectoryWatchAgentFactory();

	private HashMap<URI, PartsImageDirectoryWatchAgentThread> agents = new HashMap<URI, PartsImageDirectoryWatchAgentThread>();
	
	private final Object lock = new Object();
	
	private PartsImageDirectoryWatchAgentFactory() {
		super();
	}
	
	public static PartsImageDirectoryWatchAgentFactory getFactory() {
		return inst;
	}
	
	/**
	 * キャラクターデータを指定して、そのキャラクターデータに対する監視エージェントを作成し、そのハンドルを返します.<br>
	 * すでに、そのキャラクターデータに対するエージェントが存在する場合は、そのハンドルを返します.<br>
	 * 無効なキャラクターデータ、もしくはnullを指定した場合は、エージェントの実体を持たないダミーのハンドルが返されます.<br>
	 * @param characterData キャラクターデータ
	 * @return 作成された、もしくは既に作成されている監視エージェントのハンドル
	 */
	public PartsImageDirectoryWatchAgent getAgent(CharacterData characterData) {
		if (characterData == null || !characterData.isValid()) {
			// キャラクターデータがnullまたは無効である場合は、
			// 何もしないダミーのディレクトリハンドルを返す.
			return new NullWatchAgentHandle(characterData);
		}

		URI docBase = characterData.getDocBase();
		PartsImageDirectoryWatchAgentThread agentImpl;
		synchronized (lock) {
			if (agents.containsKey(docBase)) {
				agentImpl = agents.get(docBase);
			} else {
				agentImpl = new PartsImageDirectoryWatchAgentThread(characterData);
				agents.put(docBase, agentImpl);
				logger.log(Level.FINE, "watch agent is cached. " + agentImpl);
			}
		}
		return new PartsImageDirectoryWatchAgentHandle(agentImpl);
	}
}

/**
 * 監視スレッドの実体がない、何もしないハンドル.
 * @author seraphy
 */
class NullWatchAgentHandle implements PartsImageDirectoryWatchAgent, PartsImageDirectoryWatchListener {

	private CharacterData characterData;
	
	public NullWatchAgentHandle(CharacterData characterData) {
		this.characterData = characterData;
	}
	
	public void addPartsImageDirectoryWatchListener(
			PartsImageDirectoryWatchListener l) {
	}
	// なにもしない.
	
	public void connect() {
		// なにもしない.
	}
	
	public void detectPartsImageChange(PartsImageDirectoryWatchEvent e) {
		// なにもしない.
	}
	
	public void disconnect() {
		// なにもしない.
	}
	
	public CharacterData getCharcterData() {
		return characterData;
	}
	
	public void removePartsImageDirectoryWatchListener(
			PartsImageDirectoryWatchListener l) {
		// なにもしない.
	}
	
	public void resume() {
		// なにもしない.
	}
	
	public void suspend() {
		// なにもしない.
	}
}

/**
 * 監視ディレクトリに対する監視スレッドの実体を複数のメインフレームで共有するための、個々のフレーム用のハンドル.<br>
 * @author seraphy
 */
class PartsImageDirectoryWatchAgentHandle implements PartsImageDirectoryWatchAgent, PartsImageDirectoryWatchListener {

	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * 監視スレッドの実体
	 */
	private final PartsImageDirectoryWatchAgentThread agent;
	
	/**
	 * 監視を通知されるリスナー
	 */
	private final LinkedList<PartsImageDirectoryWatchListener> listeners
			= new LinkedList<PartsImageDirectoryWatchListener>();

	/**
	 * このハンドルで開始要求されているか示すフラグ.
	 */
	private boolean connected;
	
	protected PartsImageDirectoryWatchAgentHandle(PartsImageDirectoryWatchAgentThread agent) {
		this.agent = agent;
	}

	public CharacterData getCharcterData() {
		return agent.getCharcterData();
	}
	
	public void connect() {
		logger.log(Level.FINE, "agent connect request. " + this);
		if ( !connected) {
			agent.addPartsImageDirectoryWatchListener(this);
			connected = true;
		}
	}

	public void disconnect() {
		logger.log(Level.FINE, "agent disconnect request. " + this);
		if (connected) {
			connected = false;
			agent.removePartsImageDirectoryWatchListener(this);
		}
	}
	
	public void suspend() {
		logger.log(Level.FINE, "agent stop request. " + this);
		agent.suspend(this);
	}

	public void resume() {
		logger.log(Level.FINE, "agent resume request. " + this);
		agent.resume(this);
	}

	public void addPartsImageDirectoryWatchListener(
			PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (listeners) {
				listeners.add(l);
			}
		}
	}

	public void removePartsImageDirectoryWatchListener(
			PartsImageDirectoryWatchListener l) {
		if (l != null) {
			synchronized (listeners) {
				listeners.remove(l);
			}
		}
	}
	
	public void detectPartsImageChange(PartsImageDirectoryWatchEvent e) {
		if (!connected) {
			logger.log(Level.FINE, "skip agent event. " + this);
			return;
		}
		logger.log(Level.FINE, "agent event occured. " + this);

		PartsImageDirectoryWatchListener[] ls;
		synchronized (listeners) {
			ls = listeners.toArray(new PartsImageDirectoryWatchListener[listeners.size()]);
		}
		for (PartsImageDirectoryWatchListener l : ls) {
			l.detectPartsImageChange(e);
		}
	}
}
