package org.ruaux.jdiscogs.data;

import org.ruaux.jdiscogs.data.xml.Release;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends CrudRepository<Release, String> {

}
