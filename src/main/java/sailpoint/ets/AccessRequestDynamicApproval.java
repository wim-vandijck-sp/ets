package sailpoint.ets;

public class AccessRequestDynamicApproval {
  private final long id;
	private final String content;

	public AccessRequestDynamicApproval(long id, String content) {
		this.id = id;
		this.content = content;
	}

  public long getId() {
		return id;
	}

	public String getContent() {
		return content;
	}
}
