//
//  HistoryEntity+CoreDataClass.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 12/8/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//

import Foundation
import CoreData


public class HistoryEntity: NSManagedObject {
	convenience init() {
		self.init(entity: CoreDataManager.instance.entityForName(entityName: "HistoryEntity"), insertInto: CoreDataManager.instance.managedObjectContext)
	}
}
