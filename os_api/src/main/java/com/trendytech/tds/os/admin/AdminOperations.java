/**
 * 
 */
package com.trendytech.tds.os.admin;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import com.trendytech.tds.os.admin.model.Bucket;
import com.trendytech.tds.os.admin.model.User;

/**
 * @author Robin
 *
 */
public interface AdminOperations {

	/**
	 * 创建用户，主要字段uid，displayName，email
	 * @param user
	 * @return
	 * @throws Exception 
	 */
	public User createUser(User user) throws Exception;
	/**
	 * 修改用户，主要字段uid(required)，displayName，email
	 * @param user
	 * @return
	 */
	public User modifyUser(User user);
	/**
	 * 删除用户，主要字段uid(required)，purgedata(true--the buckets and objects belonging to the user will also be removed.)
	 * @param user
	 * @return
	 */
	public void removeUser(String uid);
	/**
	 * 分配bucket给用户
	 * @param bucketName
	 * @param uid
	 * @return
	 */
	public void linkBucket(String bucketName,String uid,String bucketId);

	/**
	 * 取消分配bucket给用户
	 * @param bucketName
	 * @param uid
	 * @return
	 */
	public void unlinkBucket(String bucketName,String uid);
	/**
	 * 删除bucket
	 * @param bucketName
	 * @return
	 */
	public void removeBucket(String bucketName);
	/**
	 * 为用户添加权限
	 * @param uid
	 * @param caps
	 * @return
	 */
	public void addUserCapability(String uid,String caps);
	/**
	 * 取消用户权限
	 * @param uid
	 * @param caps
	 * @return
	 */
	public void removeUserCapability(String uid,String caps);
	/**
	 * 获取用户配额
	 * @param uid
	 * @param quotatype
	 * @return
	 */
	public User getUserQuota(String uid,String quotatype);
	/**
	 * 为用户设置配额
	 * @param uid
	 * @param maximumObjects
	 * @param maximumSize
	 * @return
	 */
	public void setQuota(String uid,String quotatype,long maximumObjects,long maximumSize);
	/**
	 * 获取bucket配额
	 * @param uid
	 * @param quotatype
	 * @return
	 */
	public User getQuota(String uid,String quotatype);
	/**
	 * 为用户设置配额
	 * @param uid
	 * @param maximumObjects
	 * @param maximumSize
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public User setBucketQuota(String uid,String quotatype,long maximumObjects,long maximumSize) throws ClientProtocolException, IOException;
	/**
	 * 为用户设置配额
	 * @param uid
	 * @param maximumObjects
	 * @param maximumSize
	 * @return
	 */
	//public User setQuota(String uid,String quotatype,long maximumObjects,long maximumSize);
	
	/**
	 * 获取所有用户
	 * @return
	 */
	public List<User> listUsers();
	/**
	 * 根据uid获取用户信息
	 * @param userId
	 * @return
	 * @throws Exception 
	 */
	public User getUser(String userId) throws Exception;
	
	/**
	 * 根据名称获取存储桶信息
	 * @param bucketName
	 * @return
	 */
	public Bucket getBucket(String bucketName);
	
	/**
	 * 获取所有存储桶
	 * @return
	 */
	public List<Bucket> listAllBuckets();
	/**
	 * 获取可分配给用户的存储桶
	 * @return
	 */
	public List<Bucket> listAvailableBuckets();
	/**
	 * 获取指定用户的存储桶
	 * @param uid
	 * @return
	 */
	public List<Bucket> listBuckets(String uid);
}
