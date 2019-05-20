//
//  SongObject.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 11/30/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//
//	Description of song object

import Foundation

class SongObject: NSObject, Codable{
	
	var name: String!
	var artistName: String!
	var trackID: String!
	var trackLength: Double!
	var methodId: String!
	var isListen: Int32?
	var path: String?

	func initWithDict(dict:Dictionary<String,AnyObject>) {
		self.name = dict["name"] as? String
		self.trackID = dict["id"] as! String!
		self.trackLength =  NSString(string: dict["length"] as! String).doubleValue
		self.artistName = dict["artist"] as? String!
		self.methodId = dict["methodid"] as? String!
	}

}
