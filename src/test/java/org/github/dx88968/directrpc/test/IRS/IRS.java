/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.test.IRS;

/**
 *
 * @author DX
 */
public class IRS {
    
    static IRS instance;
    
    private IRS(){
        
    }
    
    public static IRS getInstance(){
        if(instance==null){
            instance =new  IRS();
        }
        return instance;
    }
    
    public int getTax(long id){
        System.out.println("id is:"+id);
        return (int) (Math.random()*10000);
    }
    
}
