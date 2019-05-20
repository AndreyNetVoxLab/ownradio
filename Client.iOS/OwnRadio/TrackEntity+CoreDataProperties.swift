//
//  TrackEntity+CoreDataProperties.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 12/9/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//

import Foundation
import CoreData


extension TrackEntity {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<TrackEntity> {
        return NSFetchRequest<TrackEntity>(entityName: "TrackEntity");
    }

    @NSManaged public var artistName: String?
    @NSManaged public var countPlay: Int32
    @NSManaged public var path: String?
    @NSManaged public var recId: String?
    @NSManaged public var trackLength: Double
    @NSManaged public var trackName: String?
    @NSManaged public var playingDate: NSDate?

}
