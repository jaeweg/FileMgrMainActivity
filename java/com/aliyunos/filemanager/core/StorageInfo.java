package com.aliyunos.filemanager.core;

public class StorageInfo {

	public String path;  
    public String state;  
    public boolean isRemoveable;  
    public StorageInfo(String path) {  
        this.path = path;  
    }  
    public boolean isMounted() {  
        return "mounted".equals(state);  
    }
    
    public String getPath(){
    	return path;
    }
    
    public boolean getIsRemoveable(){
    	return isRemoveable;
    }
    @Override  
    public String toString() {  
        return "StorageInfo [path=" + path + ", state=" + state  
                + ", isRemoveable=" + isRemoveable + "]";  
    }  
}
