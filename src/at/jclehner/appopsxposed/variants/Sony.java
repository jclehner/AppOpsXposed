package at.jclehner.appopsxposed.variants;

public class Sony extends StockAndroid
{
	@Override
	public String manufacturer() {
		return "Sony";
	}

	@Override
	public boolean useAppOpsIntent() {
		return true;
	}
}
